# tsm-behandler

## Teknologi

- Kotlin 2.4 / JVM 21
- Ktor 3.5 (Netty, `EngineMain`)
- Gradle med versjonskatalog (`gradle/libs.versions.toml` + eksterne kataloger `ktorLibs` og `tsmKtorLibs`)
- Logback for logging
- `no.nav.tsm:ktor-core` (`NaisMonitoring`) for helsesjekker, metrikker og shutdown

## Kom i gang

Java 21 kreves. Med [mise](https://mise.jdx.dev/) installeres riktig JDK automatisk:

```sh
mise install
```

### Bygg og kjør

| Kommando              | Beskrivelse                                                  |
|-----------------------|--------------------------------------------------------------|
| `./gradlew test`      | Kjør testene                                                  |
| `./gradlew build`     | Bygg prosjektet                                               |
| `./gradlew runLocal`  | Kjør lokalt med `application-local.conf` og `logback-local.xml` |
| `./gradlew run`       | Kjør med produksjonskonfigurasjon (krever NAIS-miljøvariabler) |

`runLocal` for å kjøre localt.

## Endepunkter

Applikasjonen lytter på port `8080`. Interne endepunkter kommer fra `NaisMonitoring`:

| Sti                      | Beskrivelse                          |
|--------------------------|--------------------------------------|
| `/internal/health/alive` | Liveness-probe                       |
| `/internal/health/ready` | Readiness-probe                      |
| `/internal/metrics`      | Prometheus-metrikker                 |
| `/internal/shutdown`     | preStopHook for grasiøs nedstengning |

## Konfigurasjon

| Fil                                       | Brukes av                        |
|-------------------------------------------|----------------------------------|
| `src/main/resources/application.conf`      | NAIS (dev-gcp / prod-gcp)        |
| `src/main/resources/application-local.conf`| Lokal kjøring via `runLocal`     |
| `src/main/resources/logback.xml`           | Logging på NAIS (JSON)           |
| `src/main/resources/logback-local.xml`     | Logging lokalt                   |
