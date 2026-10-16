package no.nav.tsm.modules.behandler

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import no.nav.tsm.ktor.di.dynamicDependencies
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.behandler.api.registerBehandlerRoutes
import no.nav.tsm.modules.behandler.hpr.HprClient
import no.nav.tsm.modules.behandler.hpr.HprTokenClient
import no.nav.tsm.modules.behandler.service.BehandlerService

private val logger = logger()

fun Application.configureBehandlerModule() {
    dynamicDependencies {
        local { provide(HprTokenClient.Local::class) }
        cloud { provide(HprTokenClient.Remote::class) }
    }

    dependencies {
        provide(HprClient::class)
        provide(BehandlerRepo::class)
        provide(BehandlerService::class)
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
            logger.info("event: $ApplicationStopPreparing")
            behandlerJob.stop()
            val job = job ?: return@runBlocking

            val joined = withTimeoutOrNull(5.seconds) { job.join() } != null
            if (joined) {
                logger.info("job is joined")
                return@runBlocking
            }

            logger.warn("behandlerJob did not stop within 5s, cancelling running import")
            val cancelled = withTimeoutOrNull(5.seconds) { job.cancelAndJoin() } != null
            if (!cancelled) {
                logger.error("behandlerJob did not respond to cancellation within 5s")
            }
            scope.cancel()
        }
    }
}
