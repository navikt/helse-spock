package no.nav.helse.spock

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration

private val log = LoggerFactory.getLogger("no.nav.helse.Spock")

fun main() {
    val env = System.getenv().toMutableMap()
    Thread.currentThread().setUncaughtExceptionHandler { t, e ->
        log.error("{}", e.message, e)
    }
    // midlertidig fiks for å skrive om evt. lange miljøvariabler til korte
    env.keys.toList().forEach { key ->
        if (key.startsWith("NAIS_DATABASE_SPOCK_SPOCK_")) {
            val newKey = key.replace("NAIS_DATABASE_SPOCK_SPOCK_", "DATABASE_")
            env[newKey] = env[key]
        }
    }
    launchApp(env)
}

fun launchApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()
    val schedule = env["PAMINNELSER_SCHEDULE_SECONDS"]?.let { Duration.ofSeconds(it.toLong()) }
        ?: Duration.ofMinutes(1)

    log.info("Lager påminnelser ca. hver ${schedule.toSeconds()} sekunder")

    RapidApplication.create(env).apply {
        Forkastelser(this, dataSource)
        BogusPåminnelser(this, dataSource)
        Tilstandsendringer(this, dataSource)
        IkkePåminnelser(this, dataSource)
        Påminnelser(this, dataSource, schedule)
        UtbetalingEndret(this, dataSource)
        UtbetalingPåminnelser(this, dataSource, schedule)
        PersonAvstemminger(this, dataSource)
        PersonPåminnelser(this, dataSource, schedule)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}
