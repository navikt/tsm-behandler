package no.nav.tsm.modules.behandler

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
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

    val behandlerJob: BehandlerJob by dependencies
    configureBehandlerJob(behandlerJob)
}

internal fun Application.configureBehandlerJob(behandlerJob: BehandlerJob) {
    val job =
        launch(Dispatchers.IO + CoroutineName("BehandlerJob"), start = CoroutineStart.LAZY) {
            logger.info("Behandler job started")
            behandlerJob.start()
            logger.info("Behandler job started -> done")
        }
    dependencies {
        cleanup<BehandlerJob> {
            behandlerJob.stop()
            runBlocking { job.cancelAndJoin() }
        }
    }
    monitor.subscribe(ApplicationStarted) { application ->
        if (application == this) job.start()
    }
}
