plugins {
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"
group = "org.octopusden.octopus.publishing.ft"

description = "octopus-publishing-gradle-plugin functional tests"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.platformlib:platformlib-process-local:${project.extra["platformlib-process.version"]}")
    implementation("org.slf4j:slf4j-api:${project.extra["slf4j.version"]}")
    implementation(project(":artifactory-manager"))

    testImplementation(platform("org.junit:junit-bom:${project.extra["junit-jupiter.version"]}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:${project.extra["assertj.version"]}")
    testImplementation("org.xmlunit:xmlunit-assertj3:${project.extra["xmlunit.version"]}")
    testRuntimeOnly("ch.qos.logback:logback-classic:${project.extra["logback.version"]}")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    val pluginVersion = providers.gradleProperty("octopus-publishing.version")
        .orElse(providers.environmentVariable("OCTOPUS_PUBLISHING_VERSION"))
        .getOrElse("1.0-SNAPSHOT")
    environment("OCTOPUS_PUBLISHING_VERSION", pluginVersion)
    systemProperty("octopus-publishing.version", pluginVersion)
    testLogging.showStandardStreams = true
}
