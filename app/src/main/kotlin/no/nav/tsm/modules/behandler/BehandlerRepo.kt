package no.nav.tsm.modules.behandler

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.modules.behandler.db.HprBehandlerTable
import no.nav.tsm.modules.behandler.db.normaliserHprNummer
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.isSuspendert
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BehandlerRepo(
    private val database: Database,
) {
    private val hasData = AtomicBoolean(false)

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
                    ?.let { behandlerObjectMapper.readValue(it, Behandler::class.java) }
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
                    ?.let { behandlerObjectMapper.readValue(it, Behandler::class.java) }
            }
        }

    suspend fun save(behandler: Behandler) {
        withContext(Dispatchers.IO) {
            transaction(db = database) {
                HprBehandlerTable.upsert {
                    it[HprBehandlerTable.hprNummer] = normaliserHprNummer(behandler.hprNummer)
                    it[HprBehandlerTable.fnr] = behandler.person.nin
                    it[HprBehandlerTable.sistOppdatert] = behandler.sistOppdatert
                    it[HprBehandlerTable.suspendert] = behandler.isSuspendert()
                    it[HprBehandlerTable.data] = behandlerObjectMapper.writeValueAsString(behandler)
                    it[HprBehandlerTable.oppdatert] = OffsetDateTime.now(ZoneOffset.UTC)
                }
            }
        }
    }
}
