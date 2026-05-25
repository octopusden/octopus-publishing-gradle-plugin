package org.octopusden.octopus.publishing.ft

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.xmlunit.assertj3.XmlAssert
import java.nio.file.Files

class MultiModulePublishFT {

    @Test
    @DisplayName("plugin applied at root generates POMs in every subproject")
    fun testPomsGeneratedInEverySubproject() {
        val result = runGradle {
            testProjectName = "multi-module-publish"
            tasks = listOf(
                "clean",
                ":sub-a:generatePomFileForMavenJavaPublication",
                ":sub-b:generatePomFileForMavenJavaPublication",
            )
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val ns = mapOf("p" to "http://maven.apache.org/POM/4.0.0")
        listOf("sub-a", "sub-b").forEach { sub ->
            val pomPath = result.projectPath.resolve("$sub/build/publications/mavenJava/pom-default.xml")
            assertThat(pomPath).`as`("POM for $sub").exists()
            val pom = String(Files.readAllBytes(pomPath))
            XmlAssert.assertThat(pom).withNamespaceContext(ns).valueByXPath("//p:project/p:artifactId")
                .isEqualTo(sub)
        }
    }

    @Test
    @DisplayName("publish in each subproject depends on artifactoryPublish when ARTIFACTORY_URL is set")
    fun testPublishDependsOnArtifactoryPublishPerSubproject() {
        val result = runGradle {
            testProjectName = "multi-module-publish"
            tasks = listOf(":sub-a:publish", ":sub-b:publish", "--dry-run", "--info")
            additionalEnvVariables = mapOf(
                "ARTIFACTORY_URL" to "https://artifactory.example.invalid",
                "ARTIFACTORY_DEPLOYER_USERNAME" to "u",
                "ARTIFACTORY_DEPLOYER_PASSWORD" to "p",
            )
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val joined = result.stdout.joinToString("\n")
        assertThat(joined).contains(":sub-a:artifactoryPublish")
        assertThat(joined).contains(":sub-b:artifactoryPublish")
        assertThat(joined).contains(":sub-a:publish")
        assertThat(joined).contains(":sub-b:publish")
    }

    @Test
    @DisplayName("artifactory { publish { ... } } DSL is configured on root only; invoking one subproject's publish does not schedule sibling artifactoryPublish")
    fun testArtifactoryConfiguredOnRootOnly() {
        val result = runGradle {
            testProjectName = "multi-module-publish"
            tasks = listOf(":sub-a:publish", "--dry-run", "--info")
            additionalEnvVariables = mapOf(
                "ARTIFACTORY_URL" to "https://artifactory.example.invalid",
                "ARTIFACTORY_DEPLOYER_USERNAME" to "u",
                "ARTIFACTORY_DEPLOYER_PASSWORD" to "p",
            )
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val joined = result.stdout.joinToString("\n")
        assertThat(joined).contains(":sub-a:artifactoryPublish")
        assertThat(joined).contains(":sub-a:publish")
        assertThat(joined).doesNotContain(":sub-b:artifactoryPublish")
        assertThat(joined).doesNotContain(":sub-b:publish")
        // Root-level configuration runs exactly once, not once per subproject.
        val configureLogCount = result.stdout.count { it.contains("Configuring Artifactory publish:") }
        assertEquals(
            1,
            configureLogCount,
            "expected 'Configuring Artifactory publish' to be logged exactly once (root only), got $configureLogCount"
        )
    }
}
