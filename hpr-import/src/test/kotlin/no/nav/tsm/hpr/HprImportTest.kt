package no.nav.tsm.hpr

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import no.nav.tsm.db.migrate
import no.nav.tsm.modules.behandler.db.HprBehandlerTable
import no.nav.tsm.modules.behandler.models.AdministrativReaksjon
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.Godkjenning
import no.nav.tsm.modules.behandler.models.Helsepersonellkategori
import no.nav.tsm.modules.behandler.models.Kodeverk
import no.nav.tsm.modules.behandler.models.Periode
import no.nav.tsm.modules.behandler.models.Person
import no.nav.tsm.modules.behandler.models.Type
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.postgresql.PostgreSQLContainer

class HprImportTest {
    @TempDir lateinit var directory: Path
    private val postgres = PostgreSQLContainer("postgres:18-alpine").apply { start() }
    private val jdbcUrl = "${postgres.jdbcUrl}&user=${postgres.username}&password=${postgres.password}"
    private val dataSource = HikariDataSource(HikariConfig().apply { jdbcUrl = this@HprImportTest.jdbcUrl })
    private val database = Database.connect(dataSource)

    @AfterTest
    fun stop() {
        dataSource.close()
        postgres.stop()
    }

    private fun behandler(
        hpr: String,
        nin: String?,
        godkjenninger: List<Godkjenning> = emptyList(),
        administrativeReaksjoner: List<AdministrativReaksjon> = emptyList(),
    ) =
        Behandler(
            person = Person(nin = nin, fornavn = "Test", mellomnavn = null, etternavn = "Testesen"),
            hprNummer = hpr,
            godkjenninger = godkjenninger,
            administrativeReaksjoner = administrativeReaksjoner,
            sistOppdatert = LocalDateTime.now(),
        )

    private fun export(vararg behandlere: Behandler): HprExport {
        val file = directory.resolve("export.zip")
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("export.json"))
            zip.write(behandlerObjectMapper.writeValueAsBytes(behandlere.toList()))
            zip.closeEntry()
        }
        return HprExport(file)
    }

    private fun storedHprNummer(): List<String> =
        transaction(db = database) { HprBehandlerTable.selectAll().map { it[HprBehandlerTable.hprNummer] } }

    @Test
    fun `saves behandlere with nin from the export zip and skips those without`() {
        migrate(jdbcUrl)
        val export = export(behandler(hpr = "007", nin = "12345678910"), behandler(hpr = "2", nin = null))

        HprImport(BehandlerRepo(database)).run(export)

        assertEquals(listOf("7"), storedHprNummer())
    }
}
