plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.gradleup.shadow")
    application
}

group = "no.nav"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "no.nav.tsm.hpr.MainKt"
}

kotlin {
    jvmToolchain {
        languageVersion.set(libs.versions.jvmVersion.map { JavaLanguageVersion.of(it) })
    }
}

tasks {
    shadowJar {
        archiveVersion = ""
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles {}
    }
}
