package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.Environment
import no.nav.tsm.core.initEnv

fun Application.configureDependencies() {
    dependencies {
        provide<Environment>() { this@configureDependencies.initEnv() }
    }
}
