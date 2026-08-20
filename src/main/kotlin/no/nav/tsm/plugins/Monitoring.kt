package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.ktor.nais.NaisMonitoring
import no.nav.tsm.no.nav.tsm.modules.behandler.BehandlerRepo

fun Application.configureMonitoring() {
    val behandlerService: BehandlerRepo by dependencies
    install(NaisMonitoring) {
        ready {
            check("Data available") {
                try {
                    val r = behandlerService.hasData()
                    println("has data $r")
                    r
                } catch (ex: Exception) {
                    log.error(ex.message, ex)
                    false
                }
            }
        }
    }
}
