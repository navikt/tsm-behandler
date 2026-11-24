package no.nav.tsm.modules.behandler.hpr

import no.nav.tsm.ktor.auth.texas.Texas

sealed interface HprTokenClient {
    suspend fun token(): String

    data object Local : HprTokenClient {
        override suspend fun token(): String {
            return "token"
        }
    }

    class Remote(private val texas: Texas) : HprTokenClient {
        val scope = "nhn:hpr/extended"

        override suspend fun token(): String {
            return texas.maskinporten(scope).token
        }
    }
}
