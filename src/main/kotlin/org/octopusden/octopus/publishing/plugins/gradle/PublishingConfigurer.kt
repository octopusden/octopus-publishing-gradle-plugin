package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Ensures a `mavenJava` publication exists for the `java` software component
 * when the project has both `maven-publish` applied and a `java` component.
 *
 * Does NOT apply `maven-publish` itself — the consumer is
 * responsible for applying `maven-publish`. If `maven-publish` is never
 * applied, this configurer is a no-op.
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
