package no.nav.tsm.plugins

import Environment
import initEnv
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

fun Application.configureDependencies() {
    dependencies {
        provide<HttpClient> { configureBaseHttpClient() }
        provide<Environment> { this@configureDependencies.initEnv() }
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {}
