package no.nav.tsm.modules.core.util

import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.db.getFlyway
import org.testcontainers.postgresql.PostgreSQLContainer

class PostgresTestcontainer {
    val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }
    val username: String = postgres.username
    val password: String = postgres.password
    val url: String = postgres.jdbcUrl

    init {
        getFlyway(
                PostgresConfig(
                    jdbc = url,
                    username = username,
                    password = password,
                    schema = "public",
                )
            )
            .migrate()
    }
}
