plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "no.nav"
version = "1.0.0-SNAPSHOT"

dependencies {
    api(exposedLibs.core)
    api(exposedLibs.jdbc)
    api(exposedLibs.json)
    api(exposedLibs.java.time)
}

kotlin {
    jvmToolchain {
        languageVersion.set(libs.versions.jvmVersion.map { JavaLanguageVersion.of(it) })
    }
}
