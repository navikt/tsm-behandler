package no.nav.tsm.modules.behandler.api.models

import java.time.LocalDate
import no.nav.tsm.ktor.core.Navn

data class TsmBehandler(
    val hpr: String,
    val ident: String,
    val navn: Navn?,
    val godkjenning: List<TsmGodkjenning>,
    val suspendert: Boolean,
)

data class TsmGodkjenning(
    val autorisasjon: TsmBehandlerKode,
    val helsepersonellkategori: TsmBehandlerKode,
    val tilleggskompetanse: List<TsmBehandlerTilleggskompetanse>,
)

data class TsmBehandlerTilleggskompetanse(
    val avsluttetStatus: TsmBehandlerKode,
    val gyldig: TsmPeriode,
    val type: TsmBehandlerKode,
)

data class TsmBehandlerKode(val oid: Int, val verdi: String)

data class TsmPeriode(
    val fra: LocalDate,
    val til: LocalDate?,
)
