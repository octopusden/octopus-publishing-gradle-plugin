package org.octopusden.octopus.publishing.ft

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.xmlunit.assertj3.XmlAssert
import java.nio.file.Files
import java.util.UUID

class OctopusPublishingPluginFT {

    @Test
    @DisplayName("consumer-side `pom { … }` block populates name/description/url/licenses/scm/developers in the generated POM")
    fun testPomCustomization() {
        val result = runGradle {
            testProjectName = "simple-publish"
            tasks = listOf("clean", "generatePomFileForMavenJavaPublication")
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val pomPath = result.projectPath.resolve("build/publications/mavenJava/pom-default.xml")
        assertThat(pomPath).exists()
        val pom = String(Files.readAllBytes(pomPath))
        val ns = mapOf("p" to "http://maven.apache.org/POM/4.0.0")
        XmlAssert.assertThat(pom).withNamespaceContext(ns).valueByXPath("//p:project/p:name")
            .isEqualTo("FT Sample Library")
        XmlAssert.assertThat(pom).withNamespaceContext(ns).valueByXPath("//p:project/p:description")
            .isEqualTo("Sample library used by octopus-publishing-gradle-plugin FT")
        XmlAssert.assertThat(pom).withNamespaceContext(ns).valueByXPath("//p:project/p:url")
            .isEqualTo("https://example.com/ft-sample")
        XmlAssert.assertThat(pom).withNamespaceContext(ns).valueByXPath("//p:project/p:licenses/p:license/p:name")
            .isEqualTo("Apache-2.0")
        XmlAssert.assertThat(pom).withNamespaceContext(ns).valueByXPath("//p:project/p:scm/p:url")
            .isEqualTo("https://example.com/ft-sample.git")
        XmlAssert.assertThat(pom).withNamespaceContext(ns).valueByXPath("//p:project/p:developers/p:developer/p:id")
            .isEqualTo("ft")
    }

    @ParameterizedTest(name = "credentials via {0}")
    @CsvSource(
        "env",
        "property",
    )
    @DisplayName("credentials resolve from env or project property")
    fun testCredentialResolution(source: String) {
        val envVars = mutableMapOf("ARTIFACTORY_URL" to "https://artifactory.example.invalid")
        val props = mutableMapOf<String, String>()
        val fakePassword = "test-" + UUID.randomUUID().toString()
        when (source) {
            "env" -> {
                envVars["ARTIFACTORY_DEPLOYER_USERNAME"] = "env-user"
                envVars["ARTIFACTORY_DEPLOYER_PASSWORD"] = fakePassword
            }
            "property" -> {
                props["ARTIFACTORY_DEPLOYER_USERNAME"] = "prop-user"
                props["ARTIFACTORY_DEPLOYER_PASSWORD"] = fakePassword
            }
        }

        val result = runGradle {
            testProjectName = "simple-publish"
            tasks = listOf("help", "--info")
            additionalEnvVariables = envVars
            additionalProperties = props
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val joined = result.stdout.joinToString("\n")
        assertThat(joined).contains("Configuring Artifactory publish")
        assertThat(joined).contains("contextUrl=https://artifactory.example.invalid/artifactory")
    }

    @Test
    @DisplayName("when ARTIFACTORY_URL is absent, Artifactory configuration is skipped (no failure)")
    fun testArtifactorySkippedWhenUrlMissing() {
        val result = runGradle {
            testProjectName = "simple-publish"
            tasks = listOf("help", "--info")
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")
        val joined = result.stdout.joinToString("\n")
        assertThat(joined).contains("Artifactory URL is not provided")
    }

    @ParameterizedTest(name = "publishToReleaseRepository={0} → repoKey={1}")
    @CsvSource(
        "true,  rnd-maven-release-local",
        "false, rnd-maven-dev-local",
        ",     rnd-maven-dev-local",  // unset → dev (default)
    )
    @DisplayName("publishToReleaseRepository selects the correct repoKey")
    fun testRepoKeySelection(flag: String?, expectedRepoKey: String) {
        val props = mutableMapOf<String, String>()
        flag?.takeIf { it.isNotBlank() }?.let { props["publishToReleaseRepository"] = it.trim() }

        val result = runGradle {
            testProjectName = "simple-publish"
            tasks = listOf("help", "--info")
            additionalEnvVariables = mapOf("ARTIFACTORY_URL" to "https://artifactory.example.invalid")
            additionalProperties = props
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val joined = result.stdout.joinToString("\n")
        assertThat(joined).contains("repoKey=$expectedRepoKey")
    }

    @ParameterizedTest(name = "custom extension repoKey: publishToReleaseRepository={0} → repoKey={1}")
    @CsvSource(
        ",     my-custom-dev",
        "true, my-custom-release",
    )
    @DisplayName("custom devRepoKey / releaseRepoKey from octopusPublishing { … } are honored (regression: configureRoot must defer until afterEvaluate)")
    fun testCustomRepoKeyFromExtension(flag: String?, expectedRepoKey: String) {
        val props = mutableMapOf<String, String>()
        flag?.takeIf { it.isNotBlank() }?.let { props["publishToReleaseRepository"] = it.trim() }

        val result = runGradle {
            testProjectName = "custom-repo-key"
            tasks = listOf("help", "--info")
            additionalEnvVariables = mapOf("ARTIFACTORY_URL" to "https://artifactory.example.invalid")
            additionalProperties = props
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val joined = result.stdout.joinToString("\n")
        assertThat(joined).contains("repoKey=$expectedRepoKey")
    }
}
