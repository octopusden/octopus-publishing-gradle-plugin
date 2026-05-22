package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Applies the `pomDefaults` block from [OctopusPublishingExtension] to every
 * `MavenPublication`'s POM via `withType<MavenPublication>().configureEach`.
 */
object PomCustomizer {

    fun configure(project: Project, extension: OctopusPublishingExtension) {
        project.plugins.withType(MavenPublishPlugin::class.java) {
            val publishing = project.extensions.getByType(PublishingExtension::class.java)
            publishing.publications.withType(MavenPublication::class.java).configureEach { publication ->
                publication.pom { pom ->
                    extension.pomDefaults.applyTo(pom)
                }
            }
        }
    }
}
