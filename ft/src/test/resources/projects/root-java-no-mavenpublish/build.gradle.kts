// Root has `java-library` + the publishing plugin id, but does NOT explicitly
// apply `maven-publish`. The plugin auto-applies `maven-publish` on the root
// (rm-plugin parity) and PublishingConfigurer creates `mavenJava` from the
// java component — so the ROOT itself produces a publication.
plugins {
    `java-library`
    id("org.octopusden.octopus-publishing")
}

group = "org.octopusden.publishing.ft.rootjava"
version = (project.findProperty("buildVersion") as? String) ?: "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
