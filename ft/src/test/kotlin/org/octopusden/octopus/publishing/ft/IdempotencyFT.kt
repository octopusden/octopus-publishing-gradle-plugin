package org.octopusden.octopus.publishing.ft

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Applies `id("org.octopusden.octopus-publishing")` in BOTH the root and a
 * subproject. Verifies the idempotency guard prevents double configuration
 * and that the build still produces a correct POM exactly once.
 */
class IdempotencyFT {

    @Test
    @DisplayName("plugin id applied on root and child does not double-configure or fail")
    fun testPluginIsIdempotent() {
        val result = runGradle {
            testProjectName = "idempotency"
            tasks = listOf("clean", ":child:generatePomFileForMavenJavaPublication")
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val pomPath = result.projectPath.resolve("child/build/publications/mavenJava/pom-default.xml")
        assertThat(pomPath).exists()
        val pom = String(Files.readAllBytes(pomPath))
        assertThat(pom).contains("<artifactId>child</artifactId>")
        // pomDefaults from the root extension flow into the child publication once (and only once).
        assertThat(pom).contains("https://example.com/idempotency")
        // Sanity: the <url> element appears exactly once — no double-customization.
        assertThat(pom.split("<url>").size - 1).isEqualTo(1)
    }
}
