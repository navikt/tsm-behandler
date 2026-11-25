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

dependencies {
    implementation(project(":shared"))
    implementation(libs.bigquery)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.logback.classic)
    implementation(libs.logback.encoder)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(project(":db-migration"))
    testImplementation(libs.flyway.core)
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

    test {
        useJUnitPlatform()
    }
}
