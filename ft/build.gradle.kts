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
    implementation("com.platformlib:platformlib-process-local:${project.property("platformlib-process.version")}")
    implementation("org.slf4j:slf4j-api:${project.property("slf4j.version")}")

    testImplementation(platform("org.junit:junit-bom:${project.property("junit-jupiter.version")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:${project.property("assertj.version")}")
    testImplementation("org.xmlunit:xmlunit-assertj3:${project.property("xmlunit.version")}")
    testRuntimeOnly("ch.qos.logback:logback-classic:${project.property("logback.version")}")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    dependsOn(rootProject.tasks.named("publishToMavenLocal"))
    useJUnitPlatform()
    val pluginVersion = providers.gradleProperty("octopus-publishing.version")
        .orElse(providers.environmentVariable("OCTOPUS_PUBLISHING_VERSION"))
        .getOrElse(rootProject.version.toString())
    environment("OCTOPUS_PUBLISHING_VERSION", pluginVersion)
    systemProperty("octopus-publishing.version", pluginVersion)
    testLogging.showStandardStreams = true
}
