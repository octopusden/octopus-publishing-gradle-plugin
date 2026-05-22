plugins {
    `java-library`
    `maven-publish`
    id("org.octopusden.octopus-publishing")
}

group = "org.octopusden.publishing.ft.consumer"
version = (project.findProperty("buildVersion") as? String) ?: "1.0-SNAPSHOT"
description = "Simple publishing FT consumer project"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("FT Sample Library")
                description.set("Sample library used by octopus-publishing-gradle-plugin FT")
                url.set("https://example.com/ft-sample")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://example.com/ft-sample.git")
                }
                developers {
                    developer {
                        id.set("ft")
                        name.set("FT Developer")
                    }
                }
            }
        }
    }
}
