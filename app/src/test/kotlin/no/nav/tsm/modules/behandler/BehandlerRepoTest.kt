package no.nav.tsm.modules.behandler

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import no.nav.tsm.modules.behandler.db.HprBehandlerTable
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.Person
import no.nav.tsm.modules.core.util.PostgresTestcontainer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Before
import org.junit.Test

class BehandlerRepoTest {

    companion object {
        private val container = PostgresTestcontainer()
        private val dataSource =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = container.url
                    maximumPoolSize = 5
                },
            )
        private val database = Database.connect(dataSource)
    }

    private lateinit var repo: BehandlerRepo

    @Before
    fun setup() {
        repo = BehandlerRepo(database)
        transaction(db = database) { HprBehandlerTable.deleteAll() }
    }

    @Test
    fun `empty repo has no data and returns null on lookup`() = runTest {
        assertFalse(repo.hasData())
        assertNull(repo.getBehandlerByHpr("7"))
        assertNull(repo.getBehandlerByFnr("12345678910"))
    }

    @Test
    fun `save behandler select from database`() = runTest {
        assertFalse(repo.hasData())
        assertNull(repo.getBehandlerByHpr("123456789"))
        assertNull(repo.getBehandlerByFnr("123456789101"))

        val behandler =
            behandler(
                hpr = "123456789",
                nin = "123456789101",
                sistOppdatert = LocalDateTime.now(),
            )

        repo.save(behandler)

        assertTrue(repo.hasData())
        assertNotNull(repo.getBehandlerByHpr("123456789"))
        assertNotNull(repo.getBehandlerByFnr("123456789101"))
    }

    private fun behandler(
        hpr: String,
        nin: String? = "12345678910",
        sistOppdatert: LocalDateTime = LocalDateTime.now(),
    ) =
        Behandler(
            person = Person(nin = nin, fornavn = "Test", mellomnavn = null, etternavn = "Testesen"),
            hprNummer = hpr,
            godkjenninger = emptyList(),
            administrativeReaksjoner = emptyList(),
            sistOppdatert = sistOppdatert,
        )
}
