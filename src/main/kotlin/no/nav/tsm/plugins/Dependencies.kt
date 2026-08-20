package no.nav.tsm.no.nav.tsm.plugins

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.no.nav.tsm.core.Environment
import no.nav.tsm.no.nav.tsm.core.configureEnvironment

fun Application.configureDependencies() {
    dependencies {
        provide<HttpClient> { configureBaseHttpClient() }
        provide<Environment> { this@configureDependencies.configureEnvironment() }
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {}
