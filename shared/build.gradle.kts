plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "no.nav"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain {
        languageVersion.set(libs.versions.jvmVersion.map { JavaLanguageVersion.of(it) })
    }
}
