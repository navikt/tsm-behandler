package no.nav.tsm.core

import io.ktor.server.application.Application
import io.ktor.server.config.getAs
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster

class Runtime(
    val name: String,
    val env: RuntimeCluster,
)

class Environment(runtime: Runtime)

fun Application.initEnv() =
    Environment(
        runtime =
            Runtime(
                name = environment.config.property("app.name").getAs(),
                env = getRuntimeCluster(),
            )
    )
