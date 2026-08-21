package no.nav.no.nav.tsm.modules.behandler

import junit.framework.TestCase.assertFalse
import no.nav.tsm.modules.behandler.BehandlerRepo
import org.junit.Test

class BehandlerRepoTest {
    @Test
    fun `Test behandler repository`() {
        val behandlerRepo = BehandlerRepo()
        val hasData = behandlerRepo.hasData()
        assertFalse(hasData)
    }
}
