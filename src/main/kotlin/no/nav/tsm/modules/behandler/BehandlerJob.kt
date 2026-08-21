package no.nav.tsm.modules.behandler

import java.time.LocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.measureTimedValue
import kotlinx.coroutines.*
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.behandler.hpr.HprClient
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.BehandlerDeduplicator

class BehandlerJob(val behandlerRepo: BehandlerRepo, private val hprClient: HprClient) {

    private val logger = logger()
    private val stopping = CompletableDeferred<Unit>()
    private val deduplicator = BehandlerDeduplicator()

    val stopped: Boolean
        get() = stopping.isCompleted

    private suspend fun updateData() =
        withContext(Dispatchers.IO) {
            logger.info("Started ${LocalDateTime.now()}")

            var count = 0
            val logjob =
                launch(context = Dispatchers.IO) {
                    while (isActive) {
                        logger.info("saved count :$count")
                        delay(1000)
                    }
                }
            val value = measureTimedValue {
                val capacity = behandlerRepo.size()
                val hprMap = HashMap<String, Behandler>(capacity)
                val fnrMap = HashMap<String, Behandler>(capacity)
                var reused = 0
                hprClient.getExport().collect { fresh ->
                    count++
                    val nin = fresh.person.nin ?: return@collect

                    val behandler =
                        behandlerRepo
                            .getbehandlerByHpr(fresh.hprNummer)
                            ?.takeIf { previous -> previous == fresh }
                            ?.also { reused++ } ?: deduplicator.deduplicate(fresh)
                    hprMap[behandler.hprNummer] = behandler
                    fnrMap[nin] = behandler
                }
                logger.info("Reused $reused of ${hprMap.size} behandlere")
                behandlerRepo.updateData(hprMap, fnrMap)
            }

            logger.info("Finished items: $count ${LocalDateTime.now()}")
            logger.info("value: ${value.duration}")

            // freeup unsuded ram
            Runtime.getRuntime().gc()
            logjob.cancel()
        }

    suspend fun start() =
        withContext(Dispatchers.IO) {
            while (isActive && !stopped) {
                updateData()
                withTimeoutOrNull(1.hours) {
                    stopping.await()
                    logger.info("behandlerJob: stopping awaited")
                }
            }
        }

    fun stop() {
        logger.info("Stopping job")
        stopping.complete(Unit)
    }
}
