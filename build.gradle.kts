import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.time.Duration

plugins {
    kotlin("jvm")
    groovy
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
    id("com.jfrog.artifactory")
}

description = "Octopus publishing gradle plugin (JFrog Artifactory)"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.jfrog.artifactory:com.jfrog.artifactory.gradle.plugin:${project.extra["com-jfrog-artifactory.version"]}")

    testImplementation(platform("org.junit:junit-bom:${project.extra["junit-jupiter.version"]}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:${project.extra["assertj.version"]}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.named<GroovyCompile>("compileGroovy") {
    dependsOn(tasks.named("compileKotlin"))
    classpath += files(tasks.named<KotlinCompile>("compileKotlin").get().destinationDirectory)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<GroovyCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("OctopusPublishingPlugin") {
            id = "org.octopusden.octopus-publishing"
            displayName = project.name
            description = project.description
            implementationClass = "org.octopusden.octopus.publishing.plugins.gradle.OctopusPublishingPlugin"
        }
    }
}

// ---------------------------------------------------------------------------
// Publishing of THIS plugin (JFrog + Sonatype + signing). Mirrors
// octopus-oc-template-gradle-plugin. The plugin itself only configures JFrog
// for its consumers — the dual publish here is for releasing the plugin.
// ---------------------------------------------------------------------------

artifactory {
    publish {
        val baseUrl = System.getenv().getOrDefault("ARTIFACTORY_URL", project.properties["artifactoryUrl"])
        if (baseUrl != null) {
            contextUrl = "$baseUrl/artifactory"
        }
        repository {
            repoKey = "rnd-maven-dev-local"
            username = System.getenv("ARTIFACTORY_DEPLOYER_USERNAME")
            password = System.getenv("ARTIFACTORY_DEPLOYER_PASSWORD")
        }
        defaults {
            publications("ALL_PUBLICATIONS")
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("MAVEN_USERNAME"))
            password.set(System.getenv("MAVEN_PASSWORD"))
        }
    }
    transitionCheckOptions {
        maxRetries.set(60)
        delayBetween.set(Duration.ofSeconds(30))
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/octopusden/octopus-publishing-gradle-plugin.git")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/octopusden/octopus-publishing-gradle-plugin.git")
                    connection.set("scm:git://github.com/octopusden/octopus-publishing-gradle-plugin.git")
                }
                developers {
                    developer {
                        id.set("octopus")
                        name.set("octopus")
                    }
                }
            }
        }
    }
}

signing {
    isRequired = System.getenv().containsKey("ORG_GRADLE_PROJECT_signingKey") &&
        System.getenv().containsKey("ORG_GRADLE_PROJECT_signingPassword")
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
