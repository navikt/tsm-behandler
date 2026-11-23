package no.nav.tsm.hpr

import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import org.slf4j.LoggerFactory

class HprExportClient(
    private val httpClient: HttpClient,
    private val tokenClient: MaskinportenTokenClient,
    private val hprEndpoint: String,
) {

    private companion object {
        const val OK = 200
        const val BYTES_PER_MB = 1024 * 1024
    }

    private val logger = LoggerFactory.getLogger(HprExportClient::class.java)

    fun download(): InputStream {
        val request =
            HttpRequest.newBuilder(URI.create("$hprEndpoint/v1/export"))
                .header("Authorization", "Bearer ${tokenClient.token()}")
                .GET()
                .build()
        val exportFile = Files.createTempFile("hpr-export", ".zip")
        try {
            logger.info("Downloading export from hpr")
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(exportFile))
            if (response.statusCode() != OK) {
                throw IllegalStateException("Could not get export from hpr: ${response.statusCode()}")
            }
            logger.info("Export downloaded: ${Files.size(exportFile) / BYTES_PER_MB} MB")
            return Files.newInputStream(exportFile, StandardOpenOption.DELETE_ON_CLOSE)
        } catch (e: Exception) {
            Files.deleteIfExists(exportFile)
            throw e
        }
    }
}
