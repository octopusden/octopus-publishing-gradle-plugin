import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.time.Duration

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
    id("com.jfrog.artifactory")
    id("org.octopusden.octopus-quality")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

description = "Octopus publishing gradle plugin (JFrog Artifactory)"

octopusQuality {
    // Repo has no jacoco/kover wiring — disable coverage verification.
    coverage {
        enabled.set(false)
    }
    // Absorb current debt via baselines, then fail on any new violation.
    kotlin {
        failOnViolation.set(true)
    }
    java {
        failOnViolation.set(true)
    }
}

// The octopus-quality convention plugin only configures *subprojects* when the build
// has any (see OctopusQualityPlugin: targets = allprojects - root). This repo keeps its
// production Kotlin in the code-bearing root (`src/main/kotlin`), so detekt/ktlint on the
// root are NOT configured by the plugin and the root would never be analysed by
// `qualityStatic` — a hollow gate for the plugin's own code. Configure the root analysers
// explicitly and fold them into `qualityStatic` so the root is genuinely gated.
detekt {
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
    ignoreFailures = false
}

ktlint {
    ignoreFailures.set(false)
    baseline.set(file("ktlint-baseline.xml"))
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

gradle.projectsEvaluated {
    tasks.named("qualityStatic").configure {
        dependsOn("detekt", "ktlintCheck")
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.jfrog.artifactory:com.jfrog.artifactory.gradle.plugin:${project.property("com-jfrog-artifactory.version")}")

    testImplementation(platform("org.junit:junit-bom:${project.property("junit-jupiter.version")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:${project.property("assertj.version")}")
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

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
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

artifactory {
    publish {
        val baseUrl = System.getenv("ARTIFACTORY_URL") ?: (project.properties["artifactoryUrl"] as? String)
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
