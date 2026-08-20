package no.nav.tsm.modules.behandler

import java.util.concurrent.atomic.AtomicReference
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.behandler.models.Behandler

class BehandlerRepo() {
    private val logger = logger()
    private val behandlerFnrMap = AtomicReference<Map<String, Behandler>>()
    private val behandlerHprMap = AtomicReference<Map<String, Behandler>>()

    fun hasData(): Boolean = behandlerHprMap.get()?.isNotEmpty() ?: false

    fun size(): Int = behandlerHprMap.get()?.size ?: 0

    fun updateData(hprMap: Map<String, Behandler>, fnrMap: Map<String, Behandler>) {
        logger.info("Replacing hpr data")
        behandlerFnrMap.set(fnrMap)
        behandlerHprMap.set(hprMap)
    }

    fun getbehandlerByFnr(fnr: String): Behandler? {
        return behandlerFnrMap.get()?.get(fnr)
    }

    fun getbehandlerByHpr(hpr: String): Behandler? {
        return behandlerHprMap.get()?.get(hpr)
    }
}
