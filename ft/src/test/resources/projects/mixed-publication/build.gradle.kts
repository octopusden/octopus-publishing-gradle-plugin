plugins {
    id("org.octopusden.octopus-publishing")
}

allprojects {
    group = "org.octopusden.publishing.ft.mixed"
    version = (project.findProperty("buildVersion") as? String) ?: "1.0-SNAPSHOT"
}
