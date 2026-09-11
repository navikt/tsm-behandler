val tsmKtorVersion = "1.3.0"
val exposedVersion = "1.5.0"
val ktorVersion = "3.5.2"
rootProject.name = "tsm-behandler"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
    }
    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:${ktorVersion}")
        create("tsmKtorLibs").from("no.nav.tsm:ktor-version-catalog:${tsmKtorVersion}")
        create("exposedLibs").from("org.jetbrains.exposed:exposed-version-catalog:${exposedVersion}")
    }
}

