package no.nav.tsm.modules.behandler.service

import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.behandler.BehandlerRepo
import no.nav.tsm.modules.behandler.api.models.TsmBehandler
import no.nav.tsm.modules.behandler.api.models.TsmBehandlerKode
import no.nav.tsm.modules.behandler.api.models.TsmBehandlerTilleggskompetanse
import no.nav.tsm.modules.behandler.api.models.TsmGodkjenning
import no.nav.tsm.modules.behandler.api.models.TsmPeriode
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.isActive
import no.nav.tsm.modules.behandler.models.isSuspendert
import no.nav.tsm.modules.behandler.models.toNavn

class BehandlerService(private val behandlerRepo: BehandlerRepo) {

    private val log = logger()

    suspend fun getBehandlerByHpr(hprNummer: String): TsmBehandler? {
        return behandlerRepo.getBehandlerByHpr(hprNummer)?.toTsmBehandler()
    }

    suspend fun getBehandlerByFnr(fnr: String): TsmBehandler? {
        return behandlerRepo.getBehandlerByFnr(fnr)?.toTsmBehandler()
    }

    private fun Behandler.toTsmBehandler(): TsmBehandler? {
        val nin = person.nin
        if (nin == null) {
            log.warn("Fant ikke ident for $hprNummer")
            return null
        }

        return TsmBehandler(
            hpr = hprNummer,
            ident = person.nin,
            navn = person.toNavn(),
            godkjenning =
                godkjenninger
                    .filter { it.isActive() }
                    .map {
                        TsmGodkjenning(
                            autorisasjon =
                                TsmBehandlerKode(
                                    oid = it.autorisasjon.kodeverk.id.toInt(),
                                    verdi = it.autorisasjon.verdi,
                                ),
                            helsepersonellkategori =
                                TsmBehandlerKode(
                                    oid = it.helsepersonellkategori.kodeverk.id.toInt(),
                                    verdi = it.helsepersonellkategori.verdi,
                                ),
                            tilleggskompetanse =
                                it.tilleggskompetanser.map { tilleggskompetanse ->
                                    TsmBehandlerTilleggskompetanse(
                                        avsluttetStatus =
                                            TsmBehandlerKode(
                                                oid = tilleggskompetanse.type.kodeverk.id.toInt(),
                                                verdi = tilleggskompetanse.type.verdi,
                                            ),
                                        gyldig =
                                            TsmPeriode(
                                                fra = tilleggskompetanse.periode.fra,
                                                til = tilleggskompetanse.periode.til,
                                            ),
                                        type =
                                            TsmBehandlerKode(
                                                oid = tilleggskompetanse.type.kodeverk.id.toInt(),
                                                verdi = tilleggskompetanse.type.verdi,
                                            ),
                                    )
                                },
                        )
                    },
            suspendert = isSuspendert(),
        )
    }
}
