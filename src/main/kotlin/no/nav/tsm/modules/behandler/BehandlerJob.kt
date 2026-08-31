package no.nav.tsm.modules.behandler

import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.behandler.hpr.HprClient

class BehandlerJob(private val behandlerRepo: BehandlerRepo, private val hprClient: HprClient) {

    private companion object {
        const val BATCH_SIZE = 1000
        const val PARSE_AHEAD_BATCHES = 4
    }

    private val logger = logger()
    private val stopping = CompletableDeferred<Unit>()

    val stopped: Boolean
        get() = stopping.isCompleted

    private suspend fun updateData() =
        withContext(Dispatchers.IO) {
            val completed = behandlerRepo.tryWithImportLock { runImport() }
            if (completed == null) {
                logger.info("Did not run import job, done by another pod")
            }
        }

    private suspend fun runImport() = coroutineScope {
        val importStart = OffsetDateTime.now()
        val importStartMillis = importStart.toInstant().toEpochMilli()
        logger.info("Import started $importStart")

        val read = AtomicInteger()
        val written = AtomicInteger()
        val logjob =
            launch(Dispatchers.IO) {
                while (isActive) {
                    val now = Instant.now().toEpochMilli()
                    val duration = (now - importStartMillis).milliseconds.inWholeSeconds
                    val speed =
                        when (duration) {
                            0L -> ""
                        else -> read.get() / duration
                        }
                    logger.info("read: ${read.get()}, written: ${written.get()}, speed $speed")
                    delay(1.seconds)
                }
            }
        try {
            val elapsed = measureTime {
                hprClient
                    .getExport()
                    .onEach { read.incrementAndGet() }
                    .filter { it.person.nin != null }
                    .chunked(BATCH_SIZE)
                    .buffer(PARSE_AHEAD_BATCHES)
                    .collect { batch ->
                        behandlerRepo.saveAll(batch, importStart)
                        written.addAndGet(batch.size)
                    }
            }

            val deleted = behandlerRepo.deleteRemoved(importStart)

            logger.info(
                "Import finished: read=${read.get()} written=${written.get()} deleted=$deleted elapsed=$elapsed"
            )
        } finally {
            logjob.cancel()
        }
    }

    suspend fun start() =
        withContext(Dispatchers.IO) {
            while (isActive && !stopped) {
                try {
                    updateData()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Import failed, retrying on next run", e)
                }
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
