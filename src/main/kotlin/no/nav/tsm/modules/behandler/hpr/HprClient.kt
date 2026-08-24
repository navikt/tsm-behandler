package no.nav.tsm.modules.behandler.hpr

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.jvm.javaio.*
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.behandlerObjectMapper
import tools.jackson.core.JsonTokenId
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactoryBuilder

class HprClient(
    private val texas: Texas,
    private val httpClient: HttpClient,
    environment: Environment,
    private val hprUrl: String = environment.hprExportUrl,
) {
    private val logger = logger()
    private val scope = "nhn:hpr/export"

    private val jsonFactory = JsonFactoryBuilder().disable(StreamReadFeature.AUTO_CLOSE_SOURCE).build()

    fun getExport() =
        flow<Behandler> {
                val token = texas.maskinporten(scope).token
                val response =
                    httpClient.get(hprUrl) {
                        bearerAuth(token)
                    }

                if (!response.status.isSuccess()) {
                    logger.error("Could not get export from hpr")
                    throw RuntimeException("Could not get export from hpr")
                }

                ZipInputStream(response.bodyAsChannel().toInputStream()).use { zip ->
                    try {
                        while (zip.nextEntry != null) {
                            val jsonParser = jsonFactory.createParser(ObjectReadContext.empty(), zip)
                            while (jsonParser.nextToken() != null) {
                                if (jsonParser.currentToken().id() == JsonTokenId.ID_START_OBJECT) {
                                    try {
                                        emit(
                                            behandlerObjectMapper.readValue(
                                                jsonParser,
                                                Behandler::class.java,
                                            )
                                        )
                                    } catch (e: Exception) {
                                        logger.error("Error while parsing json", e)
                                    }
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("Error while parsing offentlig hpr api", e)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
}
