package no.nav.tsm.hpr

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FormatOptions
import com.google.cloud.bigquery.JobInfo.WriteDisposition.WRITE_TRUNCATE
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.WriteChannelConfiguration
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path

internal class BigQueryTableLoader(private val bigQuery: BigQuery, table: TableId) {

    private val schema =
        Schema.of(
            Field.of("tom", StandardSQLTypeName.DATE),
            Field.of("fom", StandardSQLTypeName.DATE),
            Field.of("autorisasjon", StandardSQLTypeName.STRING),
            Field.of("hpr", StandardSQLTypeName.INT64),
            Field.of("navn", StandardSQLTypeName.STRING),
            Field.of("kategori", StandardSQLTypeName.STRING),
            Field.of("suspendert", StandardSQLTypeName.BOOL),
        )

    private val replaceTable =
        WriteChannelConfiguration.newBuilder(table, FormatOptions.json())
            .setSchema(schema)
            .setWriteDisposition(WRITE_TRUNCATE)
            .build()

    fun load(rowsFile: Path) {
        val channel = bigQuery.writer(replaceTable)
        Channels.newOutputStream(channel).use { Files.copy(rowsFile, it) }
        val job = checkNotNull(channel.job.waitFor()) { "BigQuery load job disappeared" }
        job.status.error?.let { error("BigQuery load job ${job.jobId.job} failed: ${it.reason}") }
    }
}
