import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.spotless)
}

group = "no.nav"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.jackson3)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.apache5)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(libs.logback.classic)
    implementation(libs.logback.encoder)
    implementation(tsmKtorLibs.core)
    implementation(tsmKtorLibs.auth)
    implementation(libs.logback.encoder)

    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles {}
        from("src/main/resources/logback.xml") {
            into("/")
        }
    }

    configure<SpotlessExtension> {
        kotlin {
            ktfmt("0.64").kotlinlangStyle().configure {
                it.setMaxWidth(120)
                it.setContinuationIndent(4)
            }
        }
        check {
            dependsOn("spotlessApply")
        }
    }
}

tasks.register<JavaExec>("runLocal") {
    group = "application"
    mainClass.set("io.ktor.server.netty.EngineMain")
    classpath = sourceSets["main"].runtimeClasspath

    args("-config=application-local.conf")
    jvmArgs("-Dio.ktor.development=true", "-Dlogback.configurationFile=logback-local.xml")
}