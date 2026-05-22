pluginManagement {
    plugins {
        kotlin("jvm") version settings.extra["kotlin.version"] as String
    }
}

rootProject.name = "octopus-publishing-gradle-plugin-ft"

include(":artifactory-manager")
