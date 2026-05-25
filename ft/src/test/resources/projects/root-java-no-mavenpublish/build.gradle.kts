// Root has `java-library` + the publishing plugin id, but does NOT explicitly
// apply `maven-publish`. The plugin auto-applies `maven-publish` on the root
// (rm-plugin parity); the consumer declares its own `mavenJava` publication.
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
