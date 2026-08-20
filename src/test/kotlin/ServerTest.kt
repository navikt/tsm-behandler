package no.nav

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import no.nav.tsm.module
import no.nav.tsm.no.nav.tsm.modules.behandler.hpr.HprClient
import no.nav.tsm.no.nav.tsm.modules.behandler.models.Behandler
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        application.dependencies {
            provide {
                mockk<HprClient>(relaxed = false).apply {
                    every { getExport() } returns flow<Behandler> {
                        emit(mockk<Behandler>(relaxed = true))
                    }
                }
            }
        }
        application.module()
        // verify server root returns 200
        assertEquals(HttpStatusCode.OK, client.get("/internal/health/alive").status)
        assertEquals(HttpStatusCode.OK, client.get("/internal/health/ready").status)
    }
}
