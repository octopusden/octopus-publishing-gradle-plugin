plugins {
    `java-library`
    `maven-publish`
}

description = "Sub B library"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
