package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Configures the {@code com.jfrog.artifactory} plugin.
 *
 * <p>Split into two entry points to mirror the RM plugin and keep build-info
 * JSON limited to actually-published modules:
 * <ul>
 *   <li>{@link #configureRoot} sets the {@code artifactory { publish { ... } }} DSL
 *       on the root project only (context URL, repo key, credentials,
 *       {@code publishBuildInfo}). Subprojects inherit this via the JFrog plugin.</li>
 *   <li>{@link #configurePerProject} performs per-project wiring:
 *       {@code publish.dependsOn(artifactoryPublish)} when {@code maven-publish}
 *       is present, or {@code artifactoryPublish.skip = true} otherwise.</li>
 * </ul>
 *
 * <p>Resolution order:
 * <ul>
 *   <li>URL: env {@code ARTIFACTORY_URL} → project property {@code artifactoryUrl}</li>
 *   <li>Username: env {@code ARTIFACTORY_DEPLOYER_USERNAME} → project property {@code ARTIFACTORY_DEPLOYER_USERNAME}</li>
 *   <li>Password: env {@code ARTIFACTORY_DEPLOYER_PASSWORD} → project property {@code ARTIFACTORY_DEPLOYER_PASSWORD}</li>
 *   <li>Repo key: extension {@code releaseRepoKey} if {@code -PpublishToReleaseRepository=true}, else {@code devRepoKey}</li>
 * </ul>
 *
 * <p>The legacy {@code mavenUser} / {@code mavenPassword} fallback from
 * {@code octopus-rm-gradle-plugin} has been intentionally dropped.
 */
class ArtifactoryConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryConfigurer.class)

    static final String URL_ENV = 'ARTIFACTORY_URL'
    static final String URL_PROP = 'artifactoryUrl'
    static final String USER_ENV = 'ARTIFACTORY_DEPLOYER_USERNAME'
    static final String PASS_ENV = 'ARTIFACTORY_DEPLOYER_PASSWORD'
    static final String PUBLISH_RELEASE_PROP = 'publishToReleaseRepository'
    static final String ARTIFACTORY_PLUGIN_ID = 'com.jfrog.artifactory'

    /**
     * Configure the {@code artifactory { publish { ... } }} block on the ROOT project
     * only. Subprojects inherit this configuration through the JFrog plugin's
     * per-project {@code artifactoryPublish} tasks. This mirrors the RM plugin and
     * ensures the JFrog build-info JSON contains module entries only for
     * subprojects whose {@code artifactoryPublish} actually runs.
     */
    static void configureRoot(Project rootProject, OctopusPublishingExtension extension) {
        def baseUrl = System.getenv(URL_ENV) ?: rootProject.findProperty(URL_PROP)?.toString()
        if (baseUrl == null || baseUrl.isBlank()) {
            LOGGER.info("Artifactory URL is not provided, configuration of {} is skipped", ARTIFACTORY_PLUGIN_ID)
            return
        }

        def username = System.getenv(USER_ENV) ?: rootProject.findProperty(USER_ENV)?.toString()
        def password = System.getenv(PASS_ENV) ?: rootProject.findProperty(PASS_ENV)?.toString()

        def releaseFlag = rootProject.findProperty(PUBLISH_RELEASE_PROP)?.toString() ?:
                System.getProperty(PUBLISH_RELEASE_PROP, System.getenv(PUBLISH_RELEASE_PROP))
        def publishToRelease = 'true'.equalsIgnoreCase(releaseFlag)
        def repoKey = publishToRelease ? extension.releaseRepoKey.get() : extension.devRepoKey.get()
        LOGGER.info("Configuring Artifactory publish: contextUrl={}/artifactory, repoKey={}", baseUrl, repoKey)

        rootProject.artifactory {
            publish {
                contextUrl = "${baseUrl}/artifactory" as String
                repository {
                    delegate.repoKey = repoKey
                    if (username != null) {
                        delegate.username = username
                    }
                    if (password != null) {
                        delegate.password = password
                    }
                }
                defaults {
                    publications('ALL_PUBLICATIONS')
                    publishPom = true
                    publishArtifacts = true
                }
                publishBuildInfo = true
            }
        }
    }

    /**
     * Per-project wiring: hook {@code publish} to {@code artifactoryPublish} when
     * {@code maven-publish} is present; otherwise mark {@code artifactoryPublish.skip = true}
     * so the aggregate deploy does not fail. Does NOT touch the
     * {@code artifactory { publish { ... } }} DSL — that is set on the root only.
     */
    static void configurePerProject(Project project) {
        project.afterEvaluate { Project p ->
            if (p.pluginManager.findPlugin('maven-publish')) {
                def publishTask = p.tasks.findByName('publish')
                def artifactoryPublishTask = p.tasks.findByName('artifactoryPublish')
                if (publishTask != null && artifactoryPublishTask != null) {
                    publishTask.dependsOn(artifactoryPublishTask)
                    // artifactoryPublish handles the upload;
                    // disable the redundant PublishToMavenRepository tasks generated by
                    // maven-publish for the artifactory-managed repository.
                    p.tasks.withType(PublishToMavenRepository.class).configureEach { t ->
                        t.enabled = false
                    }
                }
            } else {
                // RM plugin parity: project has com.jfrog.artifactory applied
                // (via setupRootPublishing) but no maven-publish — tell the
                // JFrog plugin to skip this project's contribution to the
                // build-info / deploy step so the aggregate artifactoryDeploy
                // does not fail.
                def artifactoryPublishTask = p.tasks.findByName('artifactoryPublish')
                if (artifactoryPublishTask != null) {
                    LOGGER.info("{} has no maven-publish; skipping artifactoryPublish", p)
                    artifactoryPublishTask.skip = true
                }
            }
        }
    }
}
