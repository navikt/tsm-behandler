package no.nav.tsm.db

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

private const val CONNECT_RETRIES = 5

fun main() {
    val jdbcUrl = System.getenv("PGJDBCURL")
    migrate(jdbcUrl)
}

fun migrate(jdbcUrl: String): MigrateResult =
    Flyway.configure()
        .dataSource(jdbcUrl, null, null)
        .connectRetries(CONNECT_RETRIES)
        .locations("classpath:db/migrations")
        .load()
        .migrate()
