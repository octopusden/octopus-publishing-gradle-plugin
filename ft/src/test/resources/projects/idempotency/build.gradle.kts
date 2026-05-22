plugins {
    id("org.octopusden.octopus-publishing")
}

allprojects {
    group = "org.octopusden.publishing.ft.idem"
    version = "1.0-SNAPSHOT"
}

octopusPublishing {
    pomDefaults {
        url.set("https://example.com/idempotency")
    }
}
