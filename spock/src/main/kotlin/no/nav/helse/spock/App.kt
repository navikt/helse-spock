package no.nav.helse.spock

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration

private val log = LoggerFactory.getLogger("no.nav.helse.Spock")

fun main() {
    launchApp(System.getenv())
}

fun launchApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()
    val schedule = env["PAMINNELSER_SCHEDULE_SECONDS"]?.let { Duration.ofSeconds(it.toLong()) }
        ?: Duration.ofMinutes(1)

    log.info("Lager påminnelser ca. hver ${schedule.toSeconds()} sekunder")

    RapidApplication.create(env).apply {
        Tilbakerulling(this, dataSource)
        Forkastelser(this, dataSource)
        BogusPåminnelser(this, dataSource)
        Tilstandsendringer(this, dataSource)
        IkkePåminnelser(this, dataSource)
        Påminnelser(this, dataSource, schedule)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}



