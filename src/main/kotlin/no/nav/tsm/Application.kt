package no.nav.tsm

import io.ktor.server.application.*
import io.ktor.server.netty.*
import no.nav.tsm.no.nav.tsm.modules.behandler.configureBehandlerModule
import no.nav.tsm.no.nav.tsm.plugins.configureAuthentication
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureMonitoring

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDependencies()
    configureMonitoring()
    configureDependencies()
    configureAuthentication()
    configureBehandlerModule()
}
