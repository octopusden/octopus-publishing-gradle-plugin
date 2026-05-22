plugins {
    id("org.octopusden.octopus-publishing")
}

allprojects {
    group = "org.octopusden.publishing.ft.multi"
    version = (project.findProperty("buildVersion") as String?) ?: "1.0-SNAPSHOT"
}

octopusPublishing {
    pomDefaults {
        url.set("https://example.com/multi-module")
        license("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt")
        scm("https://example.com/multi-module.git")
        developer("ft", "FT Developer")
    }
}
