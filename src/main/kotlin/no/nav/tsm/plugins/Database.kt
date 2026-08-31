package no.nav.tsm.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import no.nav.tsm.core.Environment
import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.db.getFlyway
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val env: Environment by dependencies

    getFlyway(env.postgres).migrate()

    val jdbcDataSource = createHikariDataSource(env.postgres)
    val jdbcDatabase = Database.connect(jdbcDataSource)

    monitor.subscribe(ApplicationStopped) { jdbcDataSource.close() }

    dependencies { provide<Database> { jdbcDatabase } }
    dependencies { provide<DataSource> { jdbcDataSource } }
}

private fun createHikariDataSource(postgresConfig: PostgresConfig) =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = postgresConfig.jdbc
            username = postgresConfig.username
            password = postgresConfig.password
            schema = postgresConfig.schema
            poolName = "tsm-behandler-jdbc"
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 5.seconds.inWholeMilliseconds
            idleTimeout = 10.minutes.inWholeMilliseconds
            maxLifetime = 30.minutes.inWholeMilliseconds
        }
    )
