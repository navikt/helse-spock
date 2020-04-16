package no.nav.helse.spock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration

private val log = LoggerFactory.getLogger("no.nav.helse.Spock")
val objectMapper = jacksonObjectMapper()
.registerModule(JavaTimeModule())
.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

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



