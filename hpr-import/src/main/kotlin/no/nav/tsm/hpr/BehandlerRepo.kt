package no.nav.tsm.hpr

import java.time.OffsetDateTime
import no.nav.tsm.modules.behandler.db.HprBehandlerTable
import no.nav.tsm.modules.behandler.db.normaliserHprNummer
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.isSuspendert
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BehandlerRepo(private val database: Database) {

    fun saveAll(behandlere: List<Behandler>, importStart: OffsetDateTime) {
        transaction(db = database) {
            HprBehandlerTable.batchUpsert(
                behandlere,
                HprBehandlerTable.hprNummer,
                shouldReturnGeneratedValues = false,
            ) { behandler ->
                this[HprBehandlerTable.hprNummer] = normaliserHprNummer(behandler.hprNummer)
                this[HprBehandlerTable.fnr] = behandler.person.nin
                this[HprBehandlerTable.sistOppdatert] = behandler.sistOppdatert
                this[HprBehandlerTable.suspendert] = behandler.isSuspendert()
                this[HprBehandlerTable.data] = behandlerObjectMapper.writeValueAsString(behandler)
                this[HprBehandlerTable.oppdatert] = importStart
            }
        }
    }
}
