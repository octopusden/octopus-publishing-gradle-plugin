package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.slf4j.LoggerFactory

/**
 * Configures the `com.jfrog.artifactory` plugin.
 *
 * Split into two entry points to mirror the RM plugin and keep the build-info
 * JSON limited to actually-published modules:
 *
 *  - [configureRoot] sets the `artifactory { publish { ... } }` DSL on the
 *    root project only (context URL, repo key, credentials, `publishBuildInfo`).
 *    Subprojects inherit this via the JFrog plugin.
 *  - [configurePerProject] performs per-project wiring:
 *    `publish.dependsOn(artifactoryPublish)` when `maven-publish` is present,
 *    or `artifactoryPublish.skip = true` otherwise.
 *
 * Resolution order:
 *  - URL: env `ARTIFACTORY_URL` → project property `artifactoryUrl`
 *  - Username: env `ARTIFACTORY_DEPLOYER_USERNAME` → project property of the same name
 *  - Password: env `ARTIFACTORY_DEPLOYER_PASSWORD` → project property of the same name
 *  - Repo key: extension `releaseRepoKey` if `-PpublishToReleaseRepository=true`, else `devRepoKey`
 *
 * The legacy `mavenUser` / `mavenPassword` fallback from
 * `octopus-rm-gradle-plugin` has been intentionally dropped.
 */
object ArtifactoryConfigurer {

    private val LOGGER = LoggerFactory.getLogger(ArtifactoryConfigurer::class.java)

    const val URL_ENV = "ARTIFACTORY_URL"
    const val URL_PROP = "artifactoryUrl"
    const val USER_ENV = "ARTIFACTORY_DEPLOYER_USERNAME"
    const val PASS_ENV = "ARTIFACTORY_DEPLOYER_PASSWORD"
    const val PUBLISH_RELEASE_PROP = "publishToReleaseRepository"
    const val ARTIFACTORY_PLUGIN_ID = "com.jfrog.artifactory"

    /**
     * Configure the `artifactory { publish { ... } }` block on the ROOT project
     * only. Subprojects inherit this configuration through the JFrog plugin's
     * per-project `artifactoryPublish` tasks. This mirrors the RM plugin and
     * ensures the JFrog build-info JSON contains module entries only for
     * subprojects whose `artifactoryPublish` actually runs.
     *
     * The configuration runs inside `rootProject.afterEvaluate { ... }` so
     * that user customizations in `octopusPublishing { ... }` (e.g. custom
     * `devRepoKey` / `releaseRepoKey`) are applied. Reading the extension's
     * `Property` values during plugin `apply()` would capture the defaults
     * before the consumer's build script has had a chance to set them.
     */
    fun configureRoot(rootProject: Project, extension: OctopusPublishingExtension) {
        rootProject.afterEvaluate {
            val baseUrl = System.getenv(URL_ENV) ?: rootProject.findProperty(URL_PROP)?.toString()
            if (baseUrl.isNullOrBlank()) {
                LOGGER.info("Artifactory URL is not provided, configuration of {} is skipped", ARTIFACTORY_PLUGIN_ID)
                return@afterEvaluate
            }

            val username = System.getenv(USER_ENV) ?: rootProject.findProperty(USER_ENV)?.toString()
            val password = System.getenv(PASS_ENV) ?: rootProject.findProperty(PASS_ENV)?.toString()

            val releaseFlag = rootProject.findProperty(PUBLISH_RELEASE_PROP)?.toString()
                ?: System.getProperty(PUBLISH_RELEASE_PROP, System.getenv(PUBLISH_RELEASE_PROP))
            val publishToRelease = "true".equals(releaseFlag, ignoreCase = true)
            val repoKey = if (publishToRelease) extension.releaseRepoKey.get() else extension.devRepoKey.get()
            LOGGER.info("Configuring Artifactory publish: contextUrl={}/artifactory, repoKey={}", baseUrl, repoKey)

            rootProject.extensions.configure(ArtifactoryPluginConvention::class.java) { convention ->
                convention.publish { publisher ->
                    publisher.contextUrl = "$baseUrl/artifactory"
                    publisher.repository { repo ->
                        repo.repoKey = repoKey
                        if (username != null) repo.username = username
                        if (password != null) repo.password = password
                    }
                    publisher.defaults { task ->
                        task.publications("ALL_PUBLICATIONS")
                        task.setPublishPom(true)
                        task.setPublishArtifacts(true)
                    }
                    publisher.isPublishBuildInfo = true
                }
            }
        }
    }

    /**
     * Per-project wiring: hook `publish` to `artifactoryPublish` when
     * `maven-publish` is present; otherwise mark `artifactoryPublish.skip = true`
     * so the aggregate deploy does not fail. Does NOT touch the
     * `artifactory { publish { ... } }` DSL — that is set on the root only.
     */
    fun configurePerProject(project: Project) {
        project.afterEvaluate { p ->
            if (p.pluginManager.findPlugin("maven-publish") != null) {
                val publishTask = p.tasks.findByName("publish")
                val artifactoryPublishTask = p.tasks.findByName("artifactoryPublish")
                if (publishTask != null && artifactoryPublishTask != null) {
                    publishTask.dependsOn(artifactoryPublishTask)
                    // artifactoryPublish handles the upload, so we disable every
                    // PublishToMavenRepository task to avoid duplicate uploads to
                    // the Artifactory endpoint.
                    //
                    // Trade-off (intentional, RM plugin parity): this is a blanket
                    // disable, not URL-scoped. If a consumer configures an
                    // additional non-Artifactory Maven repository in its
                    // `publishing { repositories { ... } }` block, that repo's
                    // publish task is also disabled. The plugin's contract is
                    // "Artifactory-only publishing for CI"; mixed-repo scenarios
                    // are out of scope. A URL-scoped disable was considered and
                    // rejected because string-matching `repository.url` against
                    // the Artifactory `baseUrl` introduces a silent
                    // double-upload failure mode on URL mismatches
                    // (trailing slash, host alias, http vs https).
                    p.tasks.withType(PublishToMavenRepository::class.java).configureEach { t ->
                        t.enabled = false
                    }
                }
            } else {
                // RM plugin parity: project has com.jfrog.artifactory applied
                // (via setupRootPublishing) but no maven-publish — tell the
                // JFrog plugin to skip this project's contribution to the
                // build-info / deploy step so the aggregate artifactoryDeploy
                // does not fail.
                val artifactoryPublishTask = p.tasks.findByName("artifactoryPublish") as? ArtifactoryTask
                if (artifactoryPublishTask != null) {
                    LOGGER.info("{} has no maven-publish; skipping artifactoryPublish", p)
                    artifactoryPublishTask.isSkip = true
                }
            }
        }
    }
}
