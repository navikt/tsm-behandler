package no.nav.tsm.modules.behandler.hpr

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.behandler.behandlerObjectMapper
import no.nav.tsm.modules.behandler.models.Behandler

class HprClient(
    private val hprTokenClient: HprTokenClient,
    private val httpClient: HttpClient,
    environment: Environment,
    private val hprEndpoint: String = environment.hprEndpoint,
) {

    private val log = logger()

    suspend fun getBehandler(hpr: String): Behandler? {
        val token = hprTokenClient.token()

        val response =
            httpClient.get("$hprEndpoint/v1/personerutvidet/$hpr") {
                bearerAuth(token)
            }

        if (response.status == HttpStatusCode.NotFound) {
            return null
        }

        if (!response.status.isSuccess()) {
            log.warn("Could not get behandler for hpr $hpr, statuscode: ${response.status.value}")
            throw RuntimeException("Feil ved oppslag til behandler ${response.status.value}")
        }

        return response.bodyAsBytes().let {
            behandlerObjectMapper.readValue(it, Behandler::class.java)
        }
    }
}
