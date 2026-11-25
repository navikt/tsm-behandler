package no.nav.tsm.hpr

import java.time.OffsetDateTime
import kotlin.time.measureTime
import org.slf4j.LoggerFactory

class HprImport(private val behandlerWriter: BehandlerRepo) {

    private companion object {
        const val BATCH_SIZE = 1000
        const val PROGRESS_LOG_INTERVAL = 10 * BATCH_SIZE
    }

    private val logger = LoggerFactory.getLogger(HprImport::class.java)

    fun run(export: HprExport) {
        val importStart = OffsetDateTime.now()
        logger.info("Import started $importStart")

        var read = 0
        var written = 0
        val elapsed = measureTime {
            export.read { behandlere ->
                behandlere
                    .onEach { read++ }
                    .filter { it.person.nin != null }
                    .chunked(BATCH_SIZE)
                    .forEach { batch ->
                        behandlerWriter.saveAll(batch, importStart)
                        written += batch.size
                        if (written % PROGRESS_LOG_INTERVAL == 0) {
                            logger.info("Import progress: read=$read written=$written")
                        }
                    }
            }
        }

        logger.info("Import finished: read=$read written=$written elapsed=$elapsed")
    }
}
