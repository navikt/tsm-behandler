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
    api(libs.flyway.postgres)
    api(libs.flyway.core)
    api(libs.postgresql)
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
    register<JavaExec>("migrateLocal") {
        description = "Apply migrations to the local PostgreSQL database"
        group = "application"
        dependsOn(":app:preRunLocal")
        mainClass.set(application.mainClass)
        classpath = sourceSets["main"].runtimeClasspath
        environment("PGJDBCURL", "jdbc:postgresql://localhost:7971/tsm-behandler?user=user&password=password")
    }

    shadowJar {
        archiveVersion = ""
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles {}
    }

    test {
        useJUnitPlatform()
    }
}
