package no.nav.tsm.db

import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.testcontainers.postgresql.PostgreSQLContainer

class MigrationTest {
    private val postgres = PostgreSQLContainer("postgres:18-alpine").apply { start() }
    private val jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}"

    @AfterTest
    fun stop() {
        postgres.stop()
    }

    @Test
    fun `applies all migrations to an empty database`() {
        val result = migrate(jdbcUrl)

        assertTrue(result.success)
        assertTrue(result.migrationsExecuted > 0)
        assertTrue(tableExists("hpr_behandler"))
    }

    @Test
    fun `running migrations again applies nothing`() {
        migrate(jdbcUrl)

        val result = migrate(jdbcUrl)

        assertTrue(result.success)
        assertEquals(0, result.migrationsExecuted)
    }

    private fun tableExists(table: String): Boolean =
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.metaData.getTables(null, "public", table, null).use { it.next() }
        }
}
