package no.nav.tsm.modules.behandler

import java.sql.Connection
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import no.nav.tsm.modules.behandler.db.HprBehandlerTable
import no.nav.tsm.modules.behandler.db.normaliserHprNummer
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.isSuspendert
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BehandlerRepo(
    private val database: Database,
    private val dataSource: DataSource,
) {
    private companion object {
        const val IMPORT_LOCK_ID = 13_69_420_37L
    }

    private val hasData = AtomicBoolean(false)

    suspend fun <T> tryWithImportLock(block: suspend () -> T): T? {
        val connection = dataSource.connection
        try {
            val lock = withContext(Dispatchers.IO) { connection.tryAcquireImportLock() }
            if (!lock) return null
            try {
                return block()
            } finally {
                withContext(NonCancellable + Dispatchers.IO) { connection.releaseImportLock() }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { connection.close() }
        }
    }

    private fun Connection.tryAcquireImportLock(): Boolean =
        prepareStatement("select pg_try_advisory_lock(?)").use { statement ->
            statement.setLong(1, IMPORT_LOCK_ID)
            statement.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
        }

    private fun Connection.releaseImportLock() {
        prepareStatement("select pg_advisory_unlock(?)").use { statement ->
            statement.setLong(1, IMPORT_LOCK_ID)
            statement.executeQuery().close()
        }
    }

    suspend fun hasData(): Boolean {
        if (hasData.get()) return true

        val data =
            withContext(Dispatchers.IO) {
                transaction(db = database, readOnly = true) {
                    HprBehandlerTable.select(intLiteral(1)).limit(1).firstOrNull() != null
                }
            }

        if (data) {
            hasData.set(true)
        }
        return data
    }

    suspend fun getBehandlerByHpr(hpr: String): Behandler? =
        withContext(Dispatchers.IO) {
            transaction(db = database, readOnly = true) {
                HprBehandlerTable.selectAll()
                    .where { HprBehandlerTable.hprNummer eq normaliserHprNummer(hpr) }
                    .limit(1)
                    .firstOrNull()
                    ?.get(HprBehandlerTable.data)
            }
        }

    suspend fun getBehandlerByFnr(fnr: String): Behandler? =
        withContext(Dispatchers.IO) {
            transaction(db = database, readOnly = true) {
                HprBehandlerTable.selectAll()
                    .where { HprBehandlerTable.fnr eq fnr }
                    .orderBy(HprBehandlerTable.sistOppdatert, SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()
                    ?.get(HprBehandlerTable.data)
            }
        }

    suspend fun saveAll(
        behandlers: List<Behandler>,
        importStart: OffsetDateTime,
    ) {
        if (behandlers.isEmpty()) return
        withContext(Dispatchers.IO) {
            transaction(db = database) {
                HprBehandlerTable.batchUpsert(
                    behandlers,
                    HprBehandlerTable.hprNummer,
                    shouldReturnGeneratedValues = false,
                ) { behandler ->
                    this[HprBehandlerTable.hprNummer] = normaliserHprNummer(behandler.hprNummer)
                    this[HprBehandlerTable.fnr] = behandler.person.nin
                    this[HprBehandlerTable.sistOppdatert] = behandler.sistOppdatert
                    this[HprBehandlerTable.suspendert] = behandler.isSuspendert()
                    this[HprBehandlerTable.data] = behandler
                    this[HprBehandlerTable.oppdatert] = importStart
                }
            }
        }
    }

    suspend fun deleteRemoved(importStart: OffsetDateTime): Int =
        withContext(Dispatchers.IO) {
            transaction(db = database) {
                HprBehandlerTable.deleteWhere { oppdatert less importStart }
            }
        }
}
