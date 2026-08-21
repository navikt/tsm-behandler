val tsmKtorVersion = "1.2.10"
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
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.5.2")
        create("tsmKtorLibs").from("no.nav.tsm:ktor-version-catalog:${tsmKtorVersion}")
    }
}

