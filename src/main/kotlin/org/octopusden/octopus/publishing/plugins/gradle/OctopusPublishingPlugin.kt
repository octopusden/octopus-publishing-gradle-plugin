package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.LoggerFactory

/**
 * Entry point for `id("org.octopusden.octopus-publishing")`.
 *
 * Responsibilities:
 *  - Register the [OctopusPublishingExtension] (`octopusPublishing { ... }`).
 *  - Apply `com.jfrog.artifactory` to the root project and every subproject
 *    (parity with the RM plugin's `setupRootPublishing`).
 *  - Delegate the actual `maven-publish` and Artifactory configuration to
 *    [PublishingConfigurer] and [ArtifactoryConfigurer].
 *
 * Apply this plugin on the **root project**. Applying it from a subproject is
 * tolerated (it resolves [Project.getRootProject] and bootstraps the same
 * global setup), but root-only is the supported pattern.
 */
class OctopusPublishingPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        LOGGER.info("Applying octopus-publishing-gradle-plugin to {}", project)

        val root = project.rootProject
        val extension = root.extensions.findByName(EXTENSION_NAME) as? OctopusPublishingExtension
            ?: root.extensions.create(EXTENSION_NAME, OctopusPublishingExtension::class.java)

        setupRootPublishing(project)

        if (project.isPluginAlreadyApplied()) {
            LOGGER.debug("octopus-publishing already configured on {}, skipping per-project setup", project)
            return
        }

        ArtifactoryConfigurer.configureRoot(root, extension)

        root.allprojects { p ->
            PublishingConfigurer.configure(p)
            ArtifactoryConfigurer.configurePerProject(p)
        }

        project.markPluginApplied()
    }

    private fun setupRootPublishing(project: Project) {
        val rootProject = project.rootProject
        // Use extraProperties.has(...) rather than Project.hasProperty(...) so
        // a user-supplied project property of the same name (e.g.
        // -PsetupOctopusPublishing=..., or a stray entry in gradle.properties)
        // cannot silently skip the required root configuration.
        val rootExtras = rootProject.extensions.extraProperties
        if (!rootExtras.has(SETUP_MARKER)) {
            rootExtras.set(SETUP_MARKER, true)
            rootProject.pluginManager.apply(ArtifactoryConfigurer.ARTIFACTORY_PLUGIN_ID)
            // RM plugin parity: auto-apply maven-publish to the root project
            // (subprojects must apply it themselves).
            rootProject.pluginManager.apply("maven-publish")
            rootProject.subprojects { subProject ->
                subProject.pluginManager.apply(ArtifactoryConfigurer.ARTIFACTORY_PLUGIN_ID)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OctopusPublishingPlugin::class.java)
        const val EXTENSION_NAME = "octopusPublishing"
        private const val SETUP_MARKER = "setupOctopusPublishing"
    }
}
