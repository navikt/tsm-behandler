package no.nav.tsm.modules.behandler

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.di.dynamicDependencies
import no.nav.tsm.modules.behandler.api.registerBehandlerRoutes
import no.nav.tsm.modules.behandler.hpr.HprClient
import no.nav.tsm.modules.behandler.hpr.HprTokenClient
import no.nav.tsm.modules.behandler.service.BehandlerService

fun Application.configureBehandlerModule() {
    dynamicDependencies {
        local { provide(HprTokenClient.Local::class) }
        cloud { provide(HprTokenClient.Remote::class) }
    }

    dependencies {
        provide(HprClient::class)
        provide(BehandlerRepo::class)
        provide(BehandlerService::class)
    }

    registerBehandlerRoutes()
}
