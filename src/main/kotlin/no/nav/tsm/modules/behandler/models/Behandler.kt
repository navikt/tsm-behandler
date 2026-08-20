package no.nav.tsm.no.nav.tsm.modules.behandler.models

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import java.time.LocalDateTime
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

data class Behandler(
    val person: Person,
    val hprNummer: String,
    val godkjenninger: List<Godkjenning>,
    val administrativeReaksjoner: List<AdministrativReaksjon>,
    val sistOppdatert: LocalDateTime,
)

data class Person(
    val nin: String?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class Godkjenning(
    val helsepersonellkategori: Helsepersonellkategori,
    val autorisasjon: Type,
    val periode: Periode,
    val rekvisisjonsretter: List<Rekvisisjonsrett>,
    val tilleggskompetanser: List<Tilleggskompetanse>,
    val administrativeReaksjoner: List<AdministrativReaksjon>,
)

data class AdministrativReaksjon(
    val type: Type,
    val periode: Periode,
)

data class Tilleggskompetanse(
    val type: Type,
    val periode: Periode,
)

data class Type(
    val navn: String,
    val verdi: String,
    val kodeverk: Kodeverk,
)

data class Periode(
    val fra: LocalDate,
    val til: LocalDate?,
)

data class Rekvisisjonsrett(
    val type: Type,
    val periode: Periode,
    val administrativeReaksjoner: List<AdministrativReaksjon>,
    val avsluttetStatus: Type?,
)

data class SpesialistGodkjenninger(
    val type: Type,
    val periode: Periode,
    val administrativeReaksjoner: List<AdministrativReaksjon>,
    val avsluttetStatus: Type?,
)

data class Helsepersonellkategori(
    val navn: String,
    val verdi: String,
    val kodeverk: Kodeverk,
)

data class Kodeverk(
    val id: String,
    val navn: String,
)

val behandlerObjectMapper =
    JsonMapper.builder()
        .addModule(KotlinModule.Builder().enable(KotlinFeature.StrictNullChecks).build())
        .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .build()
