package no.nav.tsm.plugins

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.initEnv

fun Application.configureDependencies() {
    dependencies {
        provide<HttpClient> { configureBaseHttpClient() }
        provide<Environment> { this@configureDependencies.initEnv() }
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {

    install(HttpTimeout) {
        connectTimeoutMillis = 60_000
    }
}
