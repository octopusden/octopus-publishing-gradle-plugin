package org.octopusden.octopus.publishing.ft

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Multi-module project where a convention plugin applies `maven-publish` to every subproject, but only
 * deployable subprojects declare an actual `MavenPublication`. The
 * non-publishing subproject must NOT contribute artifacts to the JFrog
 * build-info upload — its `artifactoryPublish` task must resolve to an empty
 * publication set.
 *
 * Regression guard against the auto-`mavenJava` creation behavior that
 * previously polluted build-info for every `java-library` subproject.
 */
class MixedPublicationFT {

    @Test
    @DisplayName("aggregate :artifactoryPublish via --dry-run includes lib-a/lib-b but internal-c uploads no artifacts (ALL_PUBLICATIONS resolves to empty)")
    fun testInternalSubprojectAbsentFromBuildInfo() {
        val result = runGradle {
            testProjectName = "mixed-publication"
            tasks = listOf("clean", "artifactoryPublish", "--dry-run", "-x", "artifactoryDeploy", "--info")
            additionalEnvVariables = mapOf(
                "ARTIFACTORY_URL" to "https://artifactory.example.invalid",
                "ARTIFACTORY_DEPLOYER_USERNAME" to "u",
                "ARTIFACTORY_DEPLOYER_PASSWORD" to "p",
            )
        }
        assertEquals(0, result.instance.exitCode, "Gradle execution failure:\n${result.stderr.joinToString("\n")}")

        val joined = result.stdout.joinToString("\n")
        // internal-c has `maven-publish` but no declared MavenPublication.
        // JFrog's ALL_PUBLICATIONS must resolve to empty for it.
        assertThat(joined).contains("None of the specified publications matched for project ':internal-c'")
        // lib-a and lib-b have real publications; they must NOT emit the empty diagnostic.
        assertThat(joined).doesNotContain("None of the specified publications matched for project ':lib-a'")
        assertThat(joined).doesNotContain("None of the specified publications matched for project ':lib-b'")
    }
}
