// Apply the plugin id again at subproject level to exercise the idempotency guard.
plugins {
    `java-library`
    `maven-publish`
    id("org.octopusden.octopus-publishing")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
