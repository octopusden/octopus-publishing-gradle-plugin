plugins {
    `java-library`
    `maven-publish`
}

description = "Sub A library"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
