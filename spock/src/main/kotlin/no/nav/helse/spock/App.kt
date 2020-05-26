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
    val spesialistPåminnelseDao = SpesialistPåminnelseDao(dataSource)
    val schedule = env["PAMINNELSER_SCHEDULE_SECONDS"]?.let { Duration.ofSeconds(it.toLong()) }
        ?: Duration.ofMinutes(1)

    log.info("Lager påminnelser ca. hver ${schedule.toSeconds()} sekunder")

    RapidApplication.create(env).apply {
        Tilbakerulling(this, dataSource)
        Tilstandsendringer(this, dataSource)
        Påminnelser(this, dataSource, spesialistPåminnelseDao, schedule)
        Oppgaveendringer(this, spesialistPåminnelseDao)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}



