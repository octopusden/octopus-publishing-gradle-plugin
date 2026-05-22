package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Entry point for {@code id 'org.octopusden.octopus-publishing'}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Register the {@code octopusPublishing} extension.</li>
 *   <li>Apply {@code com.jfrog.artifactory} to the root project and every
 *       subproject (parity with RM plugin's {@code setupRootPublishing}).</li>
 *   <li>Delegate the actual {@code maven-publish}, POM customization and
 *       Artifactory configuration to the Kotlin {@code *Configurer} classes.</li>
 * </ul>
 */
class OctopusPublishingPlugin implements Plugin<Project> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OctopusPublishingPlugin.class)
    static final String EXTENSION_NAME = "octopusPublishing"
    private static final String SETUP_MARKER = "setupOctopusPublishing"

    @Override
    void apply(Project project) {
        LOGGER.info("Applying octopus-publishing-gradle-plugin to {}", project)

        def root = project.rootProject
        def extension = root.extensions.findByName(EXTENSION_NAME) as OctopusPublishingExtension
        if (extension == null) {
            extension = root.extensions.create(EXTENSION_NAME, OctopusPublishingExtension.class)
        }

        setupRootPublishing(project)

        if (OctopusPublishingExtensionKt.isPluginAlreadyApplied(project)) {
            LOGGER.debug("octopus-publishing already configured on {}, skipping per-project setup", project)
            return
        }

        ArtifactoryConfigurer.configureRoot(root, extension)

        root.allprojects { Project p ->
            PublishingConfigurer.INSTANCE.configure(p)
            ArtifactoryConfigurer.configurePerProject(p)
        }

        OctopusPublishingExtensionKt.markPluginApplied(project)
    }

    private static void setupRootPublishing(Project project) {
        def rootProject = project.rootProject
        def rootExtras = rootProject.extensions.extraProperties
        if (!rootExtras.has(SETUP_MARKER)) {
            rootExtras.set(SETUP_MARKER, true)
            rootProject.pluginManager.apply(ArtifactoryConfigurer.ARTIFACTORY_PLUGIN_ID)
            // RM plugin parity: auto-apply maven-publish to the root project
            // (subprojects must apply it themselves).
            rootProject.pluginManager.apply('maven-publish')
            rootProject.subprojects { Project subProject ->
                subProject.pluginManager.apply(ArtifactoryConfigurer.ARTIFACTORY_PLUGIN_ID)
            }
        }
    }
}
