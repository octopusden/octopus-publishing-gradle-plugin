package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Applies the `maven-publish` plugin and ensures a `mavenJava` publication
 * exists for the `java` software component when the project produces one.
 *
 * Mirrors the legacy `octopus-rm-gradle-plugin` behavior of registering a
 * default `mavenJava` publication, but without the rm-plugin's `nexus`
 * property guard (consumer-side Sonatype is intentionally removed).
 */
object PublishingConfigurer {

    private const val DEFAULT_PUBLICATION_NAME = "mavenJava"

    fun configure(project: Project) {
        project.pluginManager.apply(MavenPublishPlugin::class.java)

        project.plugins.withType(MavenPublishPlugin::class.java) {
            project.afterEvaluate { afterEvalProject ->
                val javaComponent = afterEvalProject.components.findByName("java") ?: return@afterEvaluate
                val publishing = afterEvalProject.extensions.getByType(PublishingExtension::class.java)
                val existing = publishing.publications.findByName(DEFAULT_PUBLICATION_NAME)
                if (existing == null) {
                    publishing.publications.create(DEFAULT_PUBLICATION_NAME, MavenPublication::class.java) { pub ->
                        pub.from(javaComponent)
                    }
                }
            }
        }
    }
}
