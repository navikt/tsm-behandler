package no.nav.tsm.hpr

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.db.migrate
import no.nav.tsm.modules.behandler.db.HprBehandlerTable
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.Person
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.postgresql.PostgreSQLContainer

class HprImportTest {
    private val postgres = PostgreSQLContainer("postgres:18-alpine").apply { start() }
    private val jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}"
    private val dataSource = HikariDataSource(HikariConfig().apply { jdbcUrl = this@HprImportTest.jdbcUrl })
    private val database = Database.connect(dataSource)

    @AfterTest
    fun stop() {
        dataSource.close()
        postgres.stop()
    }

    private fun behandler(hpr: String, nin: String?) =
        Behandler(
            person = Person(nin = nin, fornavn = "Test", mellomnavn = null, etternavn = "Testesen"),
            hprNummer = hpr,
            godkjenninger = emptyList(),
            administrativeReaksjoner = emptyList(),
            sistOppdatert = LocalDateTime.now(),
        )

    private fun exportZip(vararg behandlere: Behandler): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("export.json"))
            zip.write(behandlerObjectMapper.writeValueAsBytes(behandlere.toList()))
            zip.closeEntry()
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    private fun storedHprNummer(): List<String> =
        transaction(db = database) { HprBehandlerTable.selectAll().map { it[HprBehandlerTable.hprNummer] } }

    @Test
    fun `saves behandlere with nin from the export zip and skips those without`() {
        migrate(jdbcUrl)
        val exportClient =
            mockk<HprExportClient> {
                every { download() } returns
                    exportZip(behandler(hpr = "007", nin = "12345678910"), behandler(hpr = "2", nin = null))
            }

        HprImport(exportClient, BehandlerRepo(database)).run()

        assertEquals(listOf("7"), storedHprNummer())
    }
}
