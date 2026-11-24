package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import no.nav.tsm.ktor.nais.NaisMonitoring
import no.nav.tsm.modules.behandler.BehandlerRepo

fun Application.configureMonitoring() {
    val behandlerService: BehandlerRepo by dependencies
    install(NaisMonitoring) {
        ready {
            check("Data available") {
                try {
                    behandlerService.hasData()
                } catch (ex: Exception) {
                    if (ex is CancellationException) {
                        currentCoroutineContext().ensureActive()
                    }
                    log.error(ex.message, ex)
                    false
                }
            }
        }
    }
}
