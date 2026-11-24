package no.nav.tsm.modules.core.util

import no.nav.tsm.db.migrate
import org.testcontainers.postgresql.PostgreSQLContainer

class PostgresTestcontainer {
    val postgres = PostgreSQLContainer("postgres:18-alpine").apply { start() }
    val url: String = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}"

    init {
        migrate(
            jdbcUrl = url,
        )
    }
}
