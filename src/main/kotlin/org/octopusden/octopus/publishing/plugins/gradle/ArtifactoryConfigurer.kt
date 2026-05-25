package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.slf4j.LoggerFactory

object ArtifactoryConfigurer {

    private val LOGGER = LoggerFactory.getLogger(ArtifactoryConfigurer::class.java)

    const val URL_ENV = "ARTIFACTORY_URL"
    const val URL_PROP = "artifactoryUrl"
    const val USER_ENV = "ARTIFACTORY_DEPLOYER_USERNAME"
    const val PASS_ENV = "ARTIFACTORY_DEPLOYER_PASSWORD"
    const val PUBLISH_RELEASE_PROP = "publishToReleaseRepository"
    const val ARTIFACTORY_PLUGIN_ID = "com.jfrog.artifactory"

    /**
     * Configure the `artifactory { publish { ... } }` block on the root project
     * only — subprojects inherit it through their own `artifactoryPublish` tasks.
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
     * Hook `publish` to depend on `artifactoryPublish` when `maven-publish` is
     * present, otherwise mark `artifactoryPublish.skip = true` so aggregate
     * deploys do not fail.
     */
    fun configurePerProject(project: Project) {
        project.afterEvaluate { p ->
            if (p.pluginManager.findPlugin("maven-publish") != null) {
                val publishTask = p.tasks.findByName("publish")
                val artifactoryPublishTask = p.tasks.findByName("artifactoryPublish")
                if (publishTask != null && artifactoryPublishTask != null) {
                    publishTask.dependsOn(artifactoryPublishTask)
                    p.tasks.withType(PublishToMavenRepository::class.java).configureEach { t ->
                        t.enabled = false
                    }
                }
            } else {
                val artifactoryPublishTask = p.tasks.findByName("artifactoryPublish") as? ArtifactoryTask
                if (artifactoryPublishTask != null) {
                    LOGGER.info("{} has no maven-publish; skipping artifactoryPublish", p)
                    artifactoryPublishTask.isSkip = true
                }
            }
        }
    }
}
