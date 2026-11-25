package no.nav.tsm.hpr

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import no.nav.tsm.modules.behandler.models.Behandler
import tools.jackson.core.JsonToken
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactoryBuilder

class HprExport(private val file: Path) {

    private val jsonFactory = JsonFactoryBuilder().disable(StreamReadFeature.AUTO_CLOSE_SOURCE).build()

    fun <T> read(block: (Sequence<Behandler>) -> T): T =
        ZipInputStream(Files.newInputStream(file)).use { zip -> block(behandlere(zip)) }

    private fun behandlere(zip: ZipInputStream): Sequence<Behandler> = sequence {
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
