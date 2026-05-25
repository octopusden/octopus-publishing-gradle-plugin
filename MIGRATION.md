# Migration from `octopus-rm-gradle-plugin`

For consumers upgrading from `octopus-rm-gradle-plugin` to
`octopus-publishing-gradle-plugin`. New consumers can ignore this document.

## Preserved (drop-in for the JFrog publishing surface)

| Item                                                                       | Notes                                                                                                                                                         |
|----------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Env var `ARTIFACTORY_URL`                                                  | Same name & semantics.                                                                                                                                        |
| Env vars `ARTIFACTORY_DEPLOYER_USERNAME` / `ARTIFACTORY_DEPLOYER_PASSWORD` | Same names.                                                                                                                                                   |
| Project property `artifactoryUrl`                                          | Fallback for the URL env var.                                                                                                                                 |
| Project property `publishToReleaseRepository`                              | Same flag, same effect.                                                                                                                                       |
| Default repo keys `rnd-maven-dev-local` / `rnd-maven-release-local`        | Now also overridable via the extension.                                                                                                                       |
| Publication name `mavenJava`                                               | Same publication name from `components.java`.                                                                                                                 |
| `publish.dependsOn(artifactoryPublish)` task wiring                        | Same.                                                                                                                                                         |
| `com.jfrog.artifactory` applied to root + every subproject                 | Same.                                                                                                                                                         |
| Multi-project behaviour                                                    | Root apply configures every subproject; projects without `maven-publish` get `artifactoryPublish.skip = true`. Same as `setupProjectPublishing` in RM plugin. |

## Changed

| Aspect                                      | Before (`octopus-rm-gradle-plugin`)                               | Now (`octopus-publishing-gradle-plugin`)                                                            |
|---------------------------------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Plugin id                                   | `org.octopusden.octopus-release-management`                       | `org.octopusden.octopus-publishing`                                                                 |
| Group                                       | `org.octopusden.octopus-release-management`                       | `org.octopusden.octopus.publishing`                                                                 |
| Consumer version property                   | `octopus-release-management.version`                              | `octopus-publishing.version`                                                                        |
| Extension name                              | (none — used Groovy metaClass tricks only)                        | `octopusPublishing { … }`                                                                           |
| Repository keys                             | Hard-coded `rnd-maven-{dev,release}-local`                        | Configurable via `octopusPublishing { devRepoKey/releaseRepoKey }` (same defaults)                  |
| Idempotency flag                            | `setupArtifactoryPublish` + `releaseManagementConfigurationState` | `setupOctopusPublishing` + `octopusPublishingConfigurationState`                                    |
| Min Gradle                                  | ~7.x                                                              | **9.0+** (built & tested against 9.5.1)                                                             |
| JDK target                                  | 17 (build & runtime)                                              | Build **21** (toolchain), bytecode/runtime **17**                                                   |
| JFrog Artifactory plugin                    | 4.x / 5.x                                                         | **6.0.4**                                                                                           |

## Out of scope

Dependency export, components-registry integration, CycloneDX SBOM generation,
escrow mode, Sonatype Central dual-publishing, GPG signing, `buildVersion` /
`setBuildVersion` handling, the deprecated `gradle-staging-plugin` alias, the
`mavenUser` / `mavenPassword` (and `NEXUS_USER` / `NEXUS_PASSWORD`) credential
fallbacks, and the `MavenPom.declareDependencies(...)` Groovy `metaClass`
helper are intentionally **not** part of this plugin. Consumers needing any
of them should stay on `octopus-rm-gradle-plugin`.

The plugin's own release pipeline still publishes to JFrog and Sonatype
Central with GPG signing (see [build.gradle.kts](build.gradle.kts)); that
is unrelated to what the plugin offers its consumers.
