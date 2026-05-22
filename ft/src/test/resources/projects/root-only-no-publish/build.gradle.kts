// Root has the publishing plugin applied but NO `java` component / no publication.
plugins {
    id("org.octopusden.octopus-publishing")
}

allprojects {
    group = "org.octopusden.publishing.ft.rootonly"
    version = "1.0-SNAPSHOT"
}
