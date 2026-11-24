plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.gradleup.shadow")
    application
}

group = "no.nav"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "no.nav.tsm.db.MainKt"
}

dependencies {
    implementation(libs.flyway.postgres)
    implementation(libs.flyway.core)
    implementation(libs.postgresql)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.logback.encoder)
    testImplementation(kotlin("test"))
    testImplementation(libs.testcontainers.postgresql)
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
