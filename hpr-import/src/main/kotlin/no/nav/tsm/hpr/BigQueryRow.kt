package no.nav.tsm.hpr

import java.time.LocalDate
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.Person
import no.nav.tsm.modules.behandler.models.isSuspendert

internal data class BigQueryRow(
    val hpr: Long,
    val navn: String,
    val kategori: String,
    val autorisasjon: String,
    val fom: LocalDate,
    val tom: LocalDate?,
    val suspendert: Boolean,
)

internal fun Behandler.toBigQueryRows(): List<BigQueryRow> =
    godkjenninger.map { godkjenning ->
        BigQueryRow(
            hpr = hprNummer.trim().toLong(),
            navn = person.fulltNavn(),
            kategori = godkjenning.helsepersonellkategori.verdi,
            autorisasjon = godkjenning.autorisasjon.verdi,
            fom = godkjenning.periode.fra,
            tom = godkjenning.periode.til,
            suspendert = isSuspendert(),
        )
    }

private fun Person.fulltNavn() = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")
