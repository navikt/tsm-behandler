package no.nav.tsm.hpr

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tsm.modules.behandler.models.Behandler
import java.time.OffsetDateTime
import kotlin.time.measureTime
import org.slf4j.LoggerFactory
import tools.jackson.core.JsonToken
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactoryBuilder
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import java.io.InputStream
import java.util.zip.ZipInputStream

class HprImport(private val exportClient: HprExportClient, private val behandlerWriter: BehandlerRepo) {

    private val jsonFactory = JsonFactoryBuilder().disable(StreamReadFeature.AUTO_CLOSE_SOURCE).build()

    private val behandlerObjectMapper =
        JsonMapper.builder()
            .addModule(KotlinModule.Builder().enable(KotlinFeature.StrictNullChecks).build())
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
            .build()

    private companion object {
        const val BATCH_SIZE = 1000
        const val PROGRESS_LOG_INTERVAL = 10 * BATCH_SIZE
    }

    private val logger = LoggerFactory.getLogger(HprImport::class.java)

    fun run() {
        val importStart = OffsetDateTime.now()
        logger.info("Import started $importStart")

        var read = 0
        var written = 0
        val elapsed = measureTime {
            exportClient.download().use { export ->
                readBehandlere(export)
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

    fun readBehandlere(export: InputStream): Sequence<Behandler> = sequence {
        ZipInputStream(export).use { zip ->
            var foundExport = false
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                jsonFactory.createParser(ObjectReadContext.empty(), zip).use { parser ->
                    var token = parser.nextToken()
                    check(token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
                        "Expected HPR export data in ${entry.name}"
                    }
                    foundExport = true
                    while (token != null) {
                        when (token) {
                            JsonToken.START_OBJECT -> yield(behandlerObjectMapper.readValue(parser, Behandler::class.java))
                            JsonToken.START_ARRAY, JsonToken.END_ARRAY -> Unit
                            else -> error("Unexpected token $token in ${entry.name}")
                        }
                        token = parser.nextToken()
                    }
                }
            }
            check(foundExport) { "HPR export contains no data files" }
        }
    }

}
