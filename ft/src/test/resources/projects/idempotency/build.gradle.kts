plugins {
    id("org.octopusden.octopus-publishing")
}

allprojects {
    group = "org.octopusden.publishing.ft.idem"
    version = (project.findProperty("buildVersion") as? String) ?: "1.0-SNAPSHOT"
}
