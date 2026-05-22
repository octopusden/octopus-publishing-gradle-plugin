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

    @Override
    void apply(Project project) {
        LOGGER.info("Applying octopus-publishing-gradle-plugin to {}", project)

        def extension = project.extensions.findByName(EXTENSION_NAME) as OctopusPublishingExtension
        if (extension == null) {
            extension = project.extensions.create(EXTENSION_NAME, OctopusPublishingExtension.class)
        }

        setupRootPublishing(project)

        if (OctopusPublishingExtensionKt.isPluginAlreadyApplied(project)) {
            LOGGER.debug("octopus-publishing already configured on {}, skipping per-project setup", project)
            return
        }

        // Configure publishing on root + every subproject.
        // Projects without maven-publish are no-ops;
        // Projects with com.jfrog.artifactory but no maven-publish get their artifactoryPublish task skipped so the build does not fail.
        project.rootProject.allprojects { Project p ->
            PublishingConfigurer.INSTANCE.configure(p)
            ArtifactoryConfigurer.configure(p, extension)
        }

        OctopusPublishingExtensionKt.markPluginApplied(project)
    }

    private static void setupRootPublishing(Project project) {
        def rootProject = project.rootProject
        if (!rootProject.hasProperty('setupOctopusPublishing')) {
            rootProject.ext.setupOctopusPublishing = true
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
