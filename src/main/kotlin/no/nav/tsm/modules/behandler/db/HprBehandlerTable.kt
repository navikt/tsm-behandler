package no.nav.tsm.modules.behandler.db

import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.behandlerObjectMapper
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

object HprBehandlerTable : Table("hpr_behandler") {
    val hprNummer = text("hpr_nummer")
    val fnr = text("fnr").nullable()
    val sistOppdatert = datetime("sist_oppdatert")
    val data =
        jsonb<Behandler>(
            "data",
            { behandlerObjectMapper.writeValueAsString(it) },
            { behandlerObjectMapper.readValue(it, Behandler::class.java) },
        )
    val oppdatert = timestampWithTimeZone("oppdatert")
    val suspendert = bool("suspendert")
    override val primaryKey = PrimaryKey(hprNummer)
}

fun normaliserHprNummer(hpr: String): String = hpr.trim().trimStart('0').ifEmpty { "0" }
