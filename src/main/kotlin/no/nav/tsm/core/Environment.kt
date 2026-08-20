package no.nav.tsm.core

import io.ktor.server.application.*
import io.ktor.server.config.*
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster
import kotlin.String


class Runtime(
    val name: String,
    val env: RuntimeCluster,
)

class Environment(
    val runtime: Runtime,
    val hprExportUrl: String,
)

fun Application.initEnv() =
    Environment(
        runtime =
            Runtime(
                name = environment.config.property("app.name").getAs(),
                env = getRuntimeCluster(),
            ),
        hprExportUrl = environment.config.property("external.hprExportEndpoint").getAs()
    )