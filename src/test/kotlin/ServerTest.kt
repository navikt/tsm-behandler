package no.nav

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        // loads default configuration
        configure("application-local.conf")
        // verify server root returns 200
        assertEquals(HttpStatusCode.OK, client.get("/internal/health/alive").status)
        assertEquals(HttpStatusCode.OK, client.get("/internal/health/ready").status)
    }
}
