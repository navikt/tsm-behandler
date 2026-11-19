package no.nav.tsm.modules.behandler

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
                    username = container.username
                    password = container.password
                    maximumPoolSize = 5
                },
            )
        private val database = Database.connect(dataSource)
    }

    private lateinit var repo: BehandlerRepo

    @Before
    fun setup() {
        repo = BehandlerRepo(database, dataSource)
        transaction(db = database) { HprBehandlerTable.deleteAll() }
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

    @Test
    fun `hpr number is normalised the same with and without leading zeros`() = runBlocking {
        repo.saveAll(listOf(behandler(hpr = "007")), OffsetDateTime.now())

        assertNotNull(repo.getBehandlerByHpr("7"))
        assertNotNull(repo.getBehandlerByHpr("007"))
        assertNotNull(repo.getBehandlerByHpr("0007"))
        assertNull(repo.getBehandlerByHpr("70"))
    }

    @Test
    fun `empty repo has no data and returns null on lookup`() = runBlocking {
        assertFalse(repo.hasData())
        assertNull(repo.getBehandlerByHpr("7"))
        assertNull(repo.getBehandlerByFnr("12345678910"))
    }

    @Test
    fun `only one pod holds the import lock at a time`() = runBlocking {
        val locked = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val holder = launch {
            repo.tryWithImportLock {
                locked.complete(Unit)
                release.await()
            }
        }
        locked.await()

        assertNull(repo.tryWithImportLock { "should not have run" })

        release.complete(Unit)
        holder.join()
        assertEquals("ok", repo.tryWithImportLock { "ok" })
    }
}
