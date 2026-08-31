package no.nav.tsm.modules.behandler.hpr

import no.nav.tsm.ktor.auth.texas.Texas

sealed interface HprTokenClient {
    suspend fun token(): String

    class Local : HprTokenClient {
        override suspend fun token(): String {
            return "token"
        }
    }

    class Remote(private val texas: Texas) : HprTokenClient {
        val scope = "nhn:hpr/export"

        override suspend fun token(): String {
            return texas.maskinporten(scope).token
        }
    }
}
