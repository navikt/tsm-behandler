package no.nav.tsm.core

import io.ktor.server.application.*
import io.ktor.server.config.*

class Environment(
    val hprEndpoint: String,
    val postgres: PostgresConfig,
)

class PostgresConfig(
    val jdbc: String,
    val schema: String,
)

fun Application.initEnv() =
    Environment(
        hprEndpoint = environment.config.property("external.hprExportEndpoint").getAs(),
        postgres =
            PostgresConfig(
                jdbc = environment.config.property("postgres.jdbc").getString(),
                schema = environment.config.property("postgres.schema").getString(),
            ),
    )
