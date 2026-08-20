package no.nav.tsm.no.nav.tsm.modules.behandler.models

import java.time.LocalDate

class BehandlerDeduplicator {

    private val kodeverker = HashMap<Kodeverk, Kodeverk>()
    private val typer = HashMap<Type, Type>()
    private val kategorier = HashMap<Helsepersonellkategori, Helsepersonellkategori>()
    private val reaksjoner = HashMap<AdministrativReaksjon, AdministrativReaksjon>()
    private val dates = HashMap<LocalDate, LocalDate>()

    fun deduplicate(behandler: Behandler): Behandler =
        behandler.copy(
            godkjenninger = behandler.godkjenninger.deduplicateEach { deduplicate(it) },
            administrativeReaksjoner = behandler.administrativeReaksjoner.deduplicateEach { deduplicate(it) },
        )

    private fun deduplicate(godkjenning: Godkjenning): Godkjenning =
        godkjenning.copy(
            helsepersonellkategori = deduplicate(godkjenning.helsepersonellkategori),
            autorisasjon = deduplicate(godkjenning.autorisasjon),
            periode = deduplicate(godkjenning.periode),
            rekvisisjonsretter = godkjenning.rekvisisjonsretter.deduplicateEach { deduplicate(it) },
            tilleggskompetanser = godkjenning.tilleggskompetanser.deduplicateEach { deduplicate(it) },
            administrativeReaksjoner = godkjenning.administrativeReaksjoner.deduplicateEach { deduplicate(it) },
        )

    private fun deduplicate(rett: Rekvisisjonsrett): Rekvisisjonsrett =
        rett.copy(
            type = deduplicate(rett.type),
            periode = deduplicate(rett.periode),
            administrativeReaksjoner = rett.administrativeReaksjoner.deduplicateEach { deduplicate(it) },
            avsluttetStatus = rett.avsluttetStatus?.let { deduplicate(it) },
        )

    private fun deduplicate(reaksjon: AdministrativReaksjon): AdministrativReaksjon =
        reaksjoner[reaksjon]
            ?: AdministrativReaksjon(deduplicate(reaksjon.type), deduplicate(reaksjon.periode)).also {
                reaksjoner[it] = it
            }

    private fun deduplicate(tillegg: Tilleggskompetanse): Tilleggskompetanse =
        Tilleggskompetanse(deduplicate(tillegg.type), deduplicate(tillegg.periode))

    private fun deduplicate(type: Type): Type =
        typer[type] ?: type.copy(kodeverk = deduplicate(type.kodeverk)).also { typer[it] = it }

    private fun deduplicate(kategori: Helsepersonellkategori): Helsepersonellkategori =
        kategorier[kategori] ?: kategori.copy(kodeverk = deduplicate(kategori.kodeverk)).also { kategorier[it] = it }

    private fun deduplicate(kodeverk: Kodeverk): Kodeverk = kodeverker.getOrPut(kodeverk) { kodeverk }

    private fun deduplicate(periode: Periode): Periode =
        Periode(deduplicate(periode.fra), periode.til?.let { deduplicate(it) })

    private fun deduplicate(dato: LocalDate): LocalDate = dates.getOrPut(dato) { dato }
    private inline fun <T> List<T>.deduplicateEach(transform: (T) -> T): List<T> =
        if (isEmpty()) emptyList() else map(transform)
}
