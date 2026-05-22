plugins {
    id("org.octopusden.octopus-publishing")
}

allprojects {
    group = "org.octopusden.publishing.ft.multi"
    version = (project.findProperty("buildVersion") as String?) ?: "1.0-SNAPSHOT"
}
