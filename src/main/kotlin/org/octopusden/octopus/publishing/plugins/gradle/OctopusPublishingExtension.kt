@file:JvmName("OctopusPublishingExtensionKt")

package org.octopusden.octopus.publishing.plugins.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPom
import javax.inject.Inject

/**
 * Consumer-facing DSL for octopus-publishing-gradle-plugin.
 *
 * Example (Kotlin):
 * ```
 * octopusPublishing {
 *     devRepoKey.set("rnd-maven-dev-local")
 *     releaseRepoKey.set("rnd-maven-release-local")
 *     pomDefaults {
 *         url.set("https://github.com/my-org/my-project")
 *         license("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt")
 *         scm("https://github.com/my-org/my-project.git")
 *         developer("octopus", "octopus")
 *     }
 * }
 * ```
 */
abstract class OctopusPublishingExtension @Inject constructor(objects: ObjectFactory) {

    /** Artifactory repository key used when `publishToReleaseRepository` is not `true`. */
    val devRepoKey: Property<String> = objects.property(String::class.java).convention("rnd-maven-dev-local")

    /** Artifactory repository key used when `-PpublishToReleaseRepository=true` is set. */
    val releaseRepoKey: Property<String> = objects.property(String::class.java).convention("rnd-maven-release-local")

    /** Defaults applied to every `MavenPublication`'s POM. */
    val pomDefaults: PomDefaults = objects.newInstance(PomDefaults::class.java)

    fun pomDefaults(action: Action<PomDefaults>) {
        action.execute(pomDefaults)
    }
}

/** POM defaults applied via `publishing.publications.withType<MavenPublication>().configureEach { pom { … } }`. */
abstract class PomDefaults @Inject constructor(objects: ObjectFactory) {
    val name: Property<String> = objects.property(String::class.java)
    val description: Property<String> = objects.property(String::class.java)
    val url: Property<String> = objects.property(String::class.java)

    internal val licenses: MutableList<LicenseSpec> = mutableListOf()
    internal val developers: MutableList<DeveloperSpec> = mutableListOf()
    internal var scm: ScmSpec? = null

    fun license(name: String, url: String) {
        licenses += LicenseSpec(name, url)
    }

    fun developer(id: String, name: String) {
        developers += DeveloperSpec(id, name)
    }

    fun scm(url: String, connection: String? = null, developerConnection: String? = null) {
        scm = ScmSpec(url, connection ?: "scm:git:$url", developerConnection)
    }

    internal fun applyTo(pom: MavenPom) {
        name.orNull?.let { pom.name.set(it) }
        description.orNull?.let { pom.description.set(it) }
        url.orNull?.let { pom.url.set(it) }
        if (licenses.isNotEmpty()) {
            pom.licenses { container ->
                licenses.forEach { spec ->
                    container.license { l ->
                        l.name.set(spec.name)
                        l.url.set(spec.url)
                    }
                }
            }
        }
        if (developers.isNotEmpty()) {
            pom.developers { container ->
                developers.forEach { spec ->
                    container.developer { d ->
                        d.id.set(spec.id)
                        d.name.set(spec.name)
                    }
                }
            }
        }
        scm?.let { spec ->
            pom.scm { s ->
                s.url.set(spec.url)
                s.connection.set(spec.connection)
                spec.developerConnection?.let { s.developerConnection.set(it) }
            }
        }
    }
}

internal data class LicenseSpec(val name: String, val url: String)
internal data class DeveloperSpec(val id: String, val name: String)
internal data class ScmSpec(val url: String, val connection: String, val developerConnection: String?)

const val PLUGIN_STATE_PROPERTY = "octopusPublishingConfigurationState"

fun Project.markPluginApplied() {
    rootProject.extensions.extraProperties.set(PLUGIN_STATE_PROPERTY, "applied")
}

fun Project.isPluginAlreadyApplied(): Boolean =
    rootProject.hasProperty(PLUGIN_STATE_PROPERTY)
