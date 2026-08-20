package no.nav.tsm.no.nav.tsm.modules.behandler.hpr

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.flow.flow
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.logger
import no.nav.tsm.no.nav.tsm.core.Environment
import no.nav.tsm.no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.no.nav.tsm.modules.behandler.models.behandlerObjectMapper
import tools.jackson.core.JsonTokenId
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.json.JsonFactory
import java.util.zip.ZipInputStream

class HprClient(private val texas: Texas, private val httpClient: HttpClient, environment: Environment,
                private val hprUrl: String = environment.hprExportUrl
) {
    private val logger = logger()
    private val scope = "nhn:hpr/export"

    fun getExport() =
        flow<Behandler> {
            val token = texas.maskinporten(scope).token
            httpClient
                .prepareGet(hprUrl) {
                    bearerAuth(token)
                }
                .execute {
                    if (it.status.isSuccess()) {
                        ZipInputStream(it.bodyAsChannel().toInputStream()).use { zip ->
                            try {
                                while (zip.nextEntry != null) {
                                    val jsonParser = JsonFactory().createParser(ObjectReadContext.empty(), zip)
                                    while (jsonParser.nextToken() != null) {
                                        if (jsonParser.currentToken().id() == JsonTokenId.ID_START_OBJECT) {
                                            emit(behandlerObjectMapper.readValue(jsonParser, Behandler::class.java))
                                        }
                                    }
                                }
                            } catch (cause: Throwable) {
                                logger.error("Error while parsing offentlig hpr api", cause)
                            }
                        }
                    } else {
                        logger.error("Could not download data from hpr")
                        throw RuntimeException("Could not download data from hpr")
                    }
                }
        }
}
