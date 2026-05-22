// Root has the publishing plugin applied but NO `java` component / no publication.
plugins {
    id("org.octopusden.octopus-publishing")
}

allprojects {
    group = "org.octopusden.publishing.ft.rootonly"
    version = "1.0-SNAPSHOT"
}

octopusPublishing {
    pomDefaults {
        url.set("https://example.com/root-only")
        license("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt")
    }
}
