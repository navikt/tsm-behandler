package no.nav.tsm.no.nav.tsm.modules.behandler

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.*
import no.nav.tsm.ktor.logger
import no.nav.tsm.no.nav.tsm.modules.behandler.api.registerBehandlerRoutes
import no.nav.tsm.no.nav.tsm.modules.behandler.hpr.HprClient
import kotlin.time.Duration.Companion.seconds

private val logger = logger()

fun Application.configureBehandlerModule() {

    dependencies {
        provide(HprClient::class)
        provide(BehandlerRepo::class)
        provide(BehandlerJob::class)
    }

    registerBehandlerRoutes()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("BehandlerJob"))
    val behandlerJob: BehandlerJob by dependencies
    var job: Job? = null

    monitor.subscribe(ApplicationStarted) {
        job = scope.launch {
            logger.info("Behandler job started")
            behandlerJob.start()
            logger.info("Behandler job started -> done")
        }
    }

    monitor.subscribe(ApplicationStopPreparing) {
        runBlocking {
            try {
                logger.info("event: $ApplicationStopPreparing")

                behandlerJob.stop()
                withTimeout(5.seconds) {
                    job?.join()
                    logger.info("job is joined")
                }
            } catch (e: Exception) {
                logger.error("Could not stop behandlerJob gracefully", e)
                scope.cancel()
            }
        }
    }
}
