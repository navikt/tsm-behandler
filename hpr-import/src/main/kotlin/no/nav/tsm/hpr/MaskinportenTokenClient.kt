package no.nav.tsm.hpr

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import tools.jackson.databind.json.JsonMapper

class MaskinportenTokenClient(private val httpClient: HttpClient, private val tokenEndpoint: String) {

    private companion object {
        const val SCOPE = "nhn:hpr/export"
        const val OK = 200
    }

    private val jsonMapper = JsonMapper.builder().build()

    fun token(): String {
        val body = jsonMapper.writeValueAsString(mapOf("identity_provider" to "maskinporten", "target" to SCOPE))
        val request =
            HttpRequest.newBuilder(URI.create(tokenEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != OK) {
            throw IllegalStateException("Could not get maskinporten token: ${response.statusCode()}")
        }
        return jsonMapper.readTree(response.body()).get("access_token").asString()
    }
}
