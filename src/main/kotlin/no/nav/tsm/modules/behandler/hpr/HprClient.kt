package no.nav.tsm.modules.behandler.hpr

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import no.nav.tsm.core.Environment
import no.nav.tsm.modules.behandler.models.Behandler
import no.nav.tsm.modules.behandler.models.behandlerObjectMapper
import tools.jackson.core.JsonTokenId
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactoryBuilder

class HprClient(
    private val hprTokenClient: HprTokenClient,
    private val httpClient: HttpClient,
    environment: Environment,
    private val hprUrl: String = environment.hprExportUrl,
) {
    private val jsonFactory = JsonFactoryBuilder().disable(StreamReadFeature.AUTO_CLOSE_SOURCE).build()

    fun getExport(): Flow<Behandler> = flow {
        val token = hprTokenClient.token()
        val response = httpClient.get(hprUrl) { bearerAuth(token) }

        if (!response.status.isSuccess()) {
            throw RuntimeException("Could not get export from hpr: ${response.status}")
        }

        ZipInputStream(response.bodyAsChannel().toInputStream()).use { zip ->
            while (zip.nextEntry != null) {
                val jsonParser = jsonFactory.createParser(ObjectReadContext.empty(), zip)
                while (jsonParser.nextToken() != null) {
                    if (jsonParser.currentToken().id() == JsonTokenId.ID_START_OBJECT) {
                        emit(behandlerObjectMapper.readValue(jsonParser, Behandler::class.java))
                    }
                }
            }
        }
    }
        .flowOn(Dispatchers.IO)
}
