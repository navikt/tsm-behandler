package no.nav.tsm.modules.behandler

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*

class BehandlerJobLifecycleTest {
    private class Fixture {
        val events = CopyOnWriteArrayList<String>()
        val started = CompletableDeferred<Job>()
        val cancelling = CompletableDeferred<Unit>()
        val finishCleanup = CompletableDeferred<Unit>()
        lateinit var application: Application

        val dataSource =
            mockk<HikariDataSource> {
                every { close() } answers { events.add("database closed") }
            }
        val behandlerJob =
            mockk<BehandlerJob> {
                every { stop() } answers { events.add("stop") }
                coEvery { start() } coAnswers
                    {
                        started.complete(currentCoroutineContext().job)
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                cancelling.complete(Unit)
                                finishCleanup.await()
                                events.add("import finished")
                            }
                        }
                    }
            }

        fun configure(application: Application) {
            this.application = application
            application.dependencies {
                provide<DataSource> { dataSource }
                provide<BehandlerJob> { behandlerJob }
            }
            val registeredDataSource: DataSource by application.dependencies
            val registeredJob: BehandlerJob by application.dependencies
            assertSame(dataSource, registeredDataSource)
            application.configureBehandlerJob(registeredJob)
        }
    }

    @Test
    fun `shutdown joins application owned import before closing database`() = runBlocking {
        val fixture = Fixture()
        val app = TestApplication { application { fixture.configure(this) } }
        try {
            app.start()
            val job = withTimeout(5.seconds) { fixture.started.await() }
            assertTrue(job in fixture.application.coroutineContext.job.children.toList())

            val stopping = async(Dispatchers.IO) { app.stop() }
            try {
                withTimeout(5.seconds) { fixture.cancelling.await() }
                assertFalse(job.isCompleted)
                assertEquals(listOf("stop"), fixture.events.toList())
            } finally {
                fixture.finishCleanup.complete(Unit)
            }
            withTimeout(5.seconds) { stopping.await() }

            assertTrue(job.isCompleted)
            assertEquals(listOf("stop", "import finished", "database closed"), fixture.events.toList())
        } finally {
            fixture.finishCleanup.complete(Unit)
            app.stop()
        }
    }

    @Test
    fun `failed startup before application started cancels pending import before closing database`() = runBlocking {
        val fixture = Fixture()
        val app = TestApplication {
            application {
                fixture.configure(this)
                error("startup failed")
            }
        }
        try {
            val failure = assertFails { app.start() }
            assertTrue(generateSequence(failure) { it.cause }.any { it.message == "startup failed" })
            assertEquals(listOf("stop", "database closed"), fixture.events.toList())
            assertTrue(fixture.application.coroutineContext.job.children.none())
            coVerify(exactly = 0) { fixture.behandlerJob.start() }
        } finally {
            fixture.finishCleanup.complete(Unit)
            app.stop()
        }
    }

    @Test
    fun `failed startup after import starts joins import before closing database`() = runBlocking {
        val fixture = Fixture()
        fixture.finishCleanup.complete(Unit)
        val app = TestApplication {
            application {
                fixture.configure(this)
                monitor.subscribe(ApplicationStarted) {
                    runBlocking { withTimeout(5.seconds) { fixture.started.await() } }
                    error("startup failed")
                }
            }
        }
        try {
            val failure = assertFails { app.start() }
            assertTrue(generateSequence(failure) { it.cause }.any { it.message == "startup failed" })
            assertTrue(fixture.started.await().isCompleted)
            assertEquals(listOf("stop", "import finished", "database closed"), fixture.events.toList())
        } finally {
            app.stop()
        }
    }
}
