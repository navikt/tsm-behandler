package no.nav.tsm.hpr

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.TableId
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.http.HttpClient
import java.time.Duration
import org.jetbrains.exposed.v1.jdbc.Database

private val CONNECT_TIMEOUT = Duration.ofSeconds(60)
private const val BIGQUERY_DATASET = "tsm_behandler"
private const val BIGQUERY_TABLE = "hpr_behandler"

fun main() {
    val jdbcUrl = System.getenv("READWRITE_PGJDBCURL")
    val hprEndpoint = System.getenv("HPR_EXPORT_ENDPOINT")
    val tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT")

    val httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()
    val exportClient = HprExportClient(httpClient, MaskinportenTokenClient(httpClient, tokenEndpoint), hprEndpoint)
    val bigQueryPublisher = BigQueryPublisher(createBigQueryTableLoader()::load)

    exportClient.withDownloadedExport { export ->
        createDataSource(jdbcUrl).use { dataSource ->
            HprImport(BehandlerRepo(Database.connect(dataSource))).run(export)
        }
        bigQueryPublisher.publish(export)
    }
}

private fun createDataSource(jdbcUrl: String) =
    HikariDataSource(
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            poolName = "tsm-behandler-hpr-import-jdbc"
            maximumPoolSize = 1
        },
    )

private fun createBigQueryTableLoader() =
    BigQueryTableLoader(
        BigQueryOptions.newBuilder().build().service,
        TableId.of(BIGQUERY_DATASET, BIGQUERY_TABLE),
    )
