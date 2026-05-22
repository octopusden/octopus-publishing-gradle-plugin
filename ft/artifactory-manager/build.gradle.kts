plugins {
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"
group = "org.octopusden.octopus.publishing.ft"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jfrog.artifactory.client:artifactory-java-client-services:${project.extra["artifactory-client.version"]}")
    api("org.slf4j:slf4j-api:${project.extra["slf4j.version"]}")
}

kotlin {
    jvmToolchain(17)
}
