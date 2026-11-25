package no.nav.tsm.hpr

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import no.nav.tsm.modules.behandler.models.Behandler

internal class BigQueryPublisher(private val upload: (Path) -> Unit) {

    fun publish(export: HprExport) {
        val rowsFile = Files.createTempFile("hpr-bigquery-rows-", ".ndjson.gz")
        try {
            val rows = export.read { behandlere -> writeRows(behandlere, rowsFile) }
            check(rows > 0) { "Cannot publish an empty HPR export to BigQuery" }
            upload(rowsFile)
        } finally {
            Files.deleteIfExists(rowsFile)
        }
    }

    private fun writeRows(behandlere: Sequence<Behandler>, rowsFile: Path): Int =
        GZIPOutputStream(Files.newOutputStream(rowsFile)).bufferedWriter().use { writer ->
            behandlere
                .filter { it.person.nin != null }
                .flatMap { it.toBigQueryRows() }
                .map { behandlerObjectMapper.writeValueAsString(it) }
                .onEach { writer.appendLine(it) }
                .count()
        }
}
