@file:JvmName("OctopusPublishingExtensionKt")

package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Consumer-facing DSL for octopus-publishing-gradle-plugin.
 *
 * Example (Kotlin):
 * ```
 * octopusPublishing {
 *     devRepoKey.set("rnd-maven-dev-local")
 *     releaseRepoKey.set("rnd-maven-release-local")
 * }
 * ```
 */
abstract class OctopusPublishingExtension @Inject constructor(objects: ObjectFactory) {

    /** Artifactory repository key used when `publishToReleaseRepository` is not `true`. */
    val devRepoKey: Property<String> = objects.property(String::class.java).convention("rnd-maven-dev-local")

    /** Artifactory repository key used when `-PpublishToReleaseRepository=true` is set. */
    val releaseRepoKey: Property<String> = objects.property(String::class.java).convention("rnd-maven-release-local")
}

const val PLUGIN_STATE_PROPERTY = "octopusPublishingConfigurationState"

fun Project.markPluginApplied() {
    rootProject.extensions.extraProperties.set(PLUGIN_STATE_PROPERTY, "applied")
}

fun Project.isPluginAlreadyApplied(): Boolean =
    rootProject.hasProperty(PLUGIN_STATE_PROPERTY)
