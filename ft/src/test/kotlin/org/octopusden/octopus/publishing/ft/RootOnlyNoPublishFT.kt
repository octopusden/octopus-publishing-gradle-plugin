package org.octopusden.octopus.publishing.ft

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Root has the plugin but no `java` component (no publication); two
 * java-library subprojects do publish. Verifies RM plugin parity:
 *   - root's `artifactoryPublish` is disabled (no failure);
 *   - subprojects publish normally.
 */
class RootOnlyNoPublishFT {

    @Test
    @DisplayName("root with no `java` component does not fail; root artifactoryPublish has nothing to publish")
    fun testRootArtifactoryPublishSkipped() {
        val result = runGradle {
            testProjectName = "root-only-no-publish"
            // --dry-run avoids the actual upload; we only need configuration + task graph to succeed.
            tasks = listOf(":artifactoryPublish", "--dry-run", "--info")
            additionalEnvVariables = mapOf(
                "ARTIFACTORY_URL" to "https://artifactory.example.invalid",
                "ARTIFACTORY_DEPLOYER_USERNAME" to "u",
                "ARTIFACTORY_DEPLOYER_PASSWORD" to "p",
            )
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")
        val joined = result.stdout.joinToString("\n")
        // Root has `maven-publish` (auto-applied by the plugin) but no `java`
        // component, so no publication is created. JFrog logs the no-match
        // message and the build succeeds.
        assertThat(joined).contains("None of the specified publications matched for project ':'")
        assertThat(joined).contains(":artifactoryPublish")
    }

    @Test
    @DisplayName("subprojects with maven-publish produce POMs")
    fun testSubprojectsPublishNormally() {
        val result = runGradle {
            testProjectName = "root-only-no-publish"
            tasks = listOf(
                "clean",
                ":lib-x:generatePomFileForMavenJavaPublication",
                ":lib-y:generatePomFileForMavenJavaPublication",
            )
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        listOf("lib-x", "lib-y").forEach { sub ->
            val pomPath = result.projectPath.resolve("$sub/build/publications/mavenJava/pom-default.xml")
            assertThat(pomPath).`as`("POM for $sub").exists()
            val pom = String(java.nio.file.Files.readAllBytes(pomPath))
            assertThat(pom).contains("<artifactId>$sub</artifactId>")
        }
    }
}
