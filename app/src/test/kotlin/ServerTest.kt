package no.nav

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.modules.behandler.BehandlerRepo
import no.nav.tsm.plugins.configureMonitoring

class ServerTest {

    private fun ApplicationTestBuilder.monitoringWithData(dataAvailable: Boolean) = application {
        dependencies { provide<BehandlerRepo> { mockk { coEvery { hasData() } returns dataAvailable } } }
        configureMonitoring()
    }

    @Test
    fun `ready responds 200 when data exists`() = testApplication {
        monitoringWithData(dataAvailable = true)

        assertEquals(HttpStatusCode.OK, client.get("/internal/health/alive").status)
        assertEquals(HttpStatusCode.OK, client.get("/internal/health/ready").status)
    }

    @Test
    fun `ready does not respond 200 before first import is finished`() = testApplication {
        monitoringWithData(dataAvailable = false)

        assertEquals(HttpStatusCode.OK, client.get("/internal/health/alive").status)
        assertEquals(HttpStatusCode.InternalServerError, client.get("/internal/health/ready").status)
    }
}
