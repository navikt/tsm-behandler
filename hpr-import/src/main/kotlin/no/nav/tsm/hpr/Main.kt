package no.nav.tsm.hpr

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.http.HttpClient
import java.time.Duration
import org.jetbrains.exposed.v1.jdbc.Database

private val CONNECT_TIMEOUT = Duration.ofSeconds(60)

fun main() {
    val jdbcUrl = System.getenv("READWRITE_PGJDBCURL")
    val hprEndpoint = System.getenv("HPR_EXPORT_ENDPOINT")
    val tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT")

    val httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()
    val exportClient = HprExportClient(httpClient, MaskinportenTokenClient(httpClient, tokenEndpoint), hprEndpoint)

    createDataSource(jdbcUrl).use { dataSource ->
        HprImport(exportClient, BehandlerRepo(Database.connect(dataSource))).run()
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
