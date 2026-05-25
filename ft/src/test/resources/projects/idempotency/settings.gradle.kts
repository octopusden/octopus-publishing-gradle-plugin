pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.octopusden.octopus-publishing") {
                val v = (settings.providers.gradleProperty("octopus-publishing.version").orNull
                    ?: System.getenv("OCTOPUS_PUBLISHING_VERSION")
                    ?: "1.0-SNAPSHOT")
                useVersion(v)
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "idempotency"
include("child")
