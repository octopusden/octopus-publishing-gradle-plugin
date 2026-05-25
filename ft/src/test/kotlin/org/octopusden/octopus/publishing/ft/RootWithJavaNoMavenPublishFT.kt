package org.octopusden.octopus.publishing.ft

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Root has `java-library` but does NOT apply `maven-publish` explicitly; the
 * plugin auto-applies `maven-publish` (rm-plugin parity), and the consumer
 * declares its own `mavenJava` publication.
 */
class RootWithJavaNoMavenPublishFT {

    @Test
    @DisplayName("root with `java-library` and no explicit `maven-publish` publishes the consumer-declared mavenJava publication (POM generated)")
    fun testRootMavenJavaPublicationGenerated() {
        val result = runGradle {
            testProjectName = "root-java-no-mavenpublish"
            tasks = listOf("clean", ":generatePomFileForMavenJavaPublication")
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val pomPath = result.projectPath.resolve("build/publications/mavenJava/pom-default.xml")
        assertThat(pomPath).`as`("root mavenJava POM").exists()
        val pom = String(Files.readAllBytes(pomPath))
        assertThat(pom).contains("<artifactId>root-java-no-mavenpublish</artifactId>")
        assertThat(pom).contains("<groupId>org.octopusden.publishing.ft.rootjava</groupId>")
    }

    @Test
    @DisplayName("root `:publish` schedules `:artifactoryPublish` (real publishing step exists)")
    fun testRootPublishWiredToArtifactoryPublish() {
        val result = runGradle {
            testProjectName = "root-java-no-mavenpublish"
            tasks = listOf(":publish", "--dry-run", "--info")
            additionalEnvVariables = mapOf(
                "ARTIFACTORY_URL" to "https://artifactory.example.invalid",
                "ARTIFACTORY_DEPLOYER_USERNAME" to "u",
                "ARTIFACTORY_DEPLOYER_PASSWORD" to "p",
            )
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val joined = result.stdout.joinToString("\n")
        assertThat(joined).contains(":publish")
        assertThat(joined).contains(":artifactoryPublish")
        assertThat(joined).doesNotContain("None of the specified publications matched for project ':'")
    }
}
