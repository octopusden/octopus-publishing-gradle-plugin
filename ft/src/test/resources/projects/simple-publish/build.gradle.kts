plugins {
    `java-library`
    id("org.octopusden.octopus-publishing")
}

group = "org.octopusden.publishing.ft.consumer"
version = (project.findProperty("buildVersion") as String?) ?: "1.0-SNAPSHOT"
description = "Simple publishing FT consumer project"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

octopusPublishing {
    pomDefaults {
        name.set("FT Sample Library")
        description.set("Sample library used by octopus-publishing-gradle-plugin FT")
        url.set("https://example.com/ft-sample")
        license("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt")
        scm("https://example.com/ft-sample.git")
        developer("ft", "FT Developer")
    }
}
