package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Creates a default `mavenJava` publication from the `java` component when
 * `maven-publish` is applied. No-op otherwise.
 */
object PublishingConfigurer {

    private const val DEFAULT_PUBLICATION_NAME = "mavenJava"

    fun configure(project: Project) {
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
