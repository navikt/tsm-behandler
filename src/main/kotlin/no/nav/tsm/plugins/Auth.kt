package no.nav.tsm.no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.auth.entra.EntraAuth
import no.nav.tsm.ktor.auth.texas.Texas

fun Application.configureAuthentication() {

    dependencies {
        provide(Texas::class)
    }

    install(EntraAuth) {
        autoStub = true
        machine = true
    }
}
