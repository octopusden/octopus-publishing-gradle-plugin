plugins {
    `java-library`
    id("org.octopusden.octopus-publishing")
}

group = "org.octopusden.publishing.ft.custom"
version = "1.0-SNAPSHOT"

// Customize the extension. Reading these via configureRoot's afterEvaluate
// must observe these values (not the plugin defaults).
octopusPublishing {
    devRepoKey.set("my-custom-dev")
    releaseRepoKey.set("my-custom-release")
}
