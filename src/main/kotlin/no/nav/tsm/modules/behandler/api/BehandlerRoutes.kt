package no.nav.tsm.modules.behandler.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import no.nav.tsm.ktor.auth.entra.entraMachineToken
import no.nav.tsm.modules.behandler.BehandlerRepo
import no.nav.tsm.modules.behandler.models.Behandler
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

private val behandlerApiMapper =
    JsonMapper.builder()
        .addModule(KotlinModule.Builder().enable(KotlinFeature.StrictNullChecks).build())
        .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .build()

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BehandlerQuery.HprQuery::class, name = "HPR"),
    JsonSubTypes.Type(value = BehandlerQuery.FnrQuery::class, name = "FNR"),
)
sealed interface BehandlerQuery {
    enum class QueryType {
        HPR,
        FNR,
    }

    val id: String
    val type: QueryType

    data class FnrQuery(override val id: String) : BehandlerQuery {
        override val type = QueryType.FNR
    }

    data class HprQuery(override val id: String) : BehandlerQuery {
        override val type = QueryType.HPR
    }
}

fun Application.registerBehandlerRoutes() {
    val behandlerRepo: BehandlerRepo by dependencies

    routing {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(behandlerApiMapper))
        }
        entraMachineToken {
            post("api/behandler/search") {
                val query = call.receive<BehandlerQuery>()
                val behandler: Behandler? =
                    when (query) {
                        is BehandlerQuery.FnrQuery -> behandlerRepo.getbehandlerByFnr(query.id)
                        is BehandlerQuery.HprQuery -> behandlerRepo.getbehandlerByHpr(query.id)
                    }
                if (behandler == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(behandler)
                }
            }
        }
    }
}
