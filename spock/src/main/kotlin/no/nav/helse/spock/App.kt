package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext

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

    seedApp(dataSource, env)

    RapidApplication.create(env).apply {
        Tilbakerulling(this, dataSource)
        Forkastelser(this, dataSource)
        BogusPåminnelser(this, dataSource)
        Tilstandsendringer(this, dataSource)
        IkkePåminnelser(this, dataSource)
        Påminnelser(this, dataSource, schedule)
        UtbetalingEndret(this, dataSource)
        UtbetalingPåminnelser(this, dataSource, schedule)
        PersonPåminnelser(this, dataSource, schedule)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}

private fun seedApp(dataSource: DataSource, env: Map<String, String>) {
    val kafkaConfig = KafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
        consumerGroupId = env.getValue("KAFKA_CONSUMER_GROUP_ID") + "-reseed",
        username = "/var/run/secrets/nais.io/service_user/username".readFile(),
        password = "/var/run/secrets/nais.io/service_user/password".readFile(),
        truststore = env["NAV_TRUSTSTORE_PATH"],
        truststorePassword = env["NAV_TRUSTSTORE_PASSWORD"],
        autoOffsetResetConfig = "earliest"
    )
    val seedApp = KafkaRapid.create(kafkaConfig, env.getValue("KAFKA_RAPID_TOPIC"))
        .apply {
            Runtime.getRuntime().addShutdownHook(Thread(this::stop))
            River(this).apply {
                validate {
                    it.requireAny("@event_name", listOf("vedtaksperiode_endret", "utbetaling_endret"))
                    it.requireKey("fødselsnummer", "aktørId")
                    it.require("@opprettet", JsonNode::asLocalDateTime)
                }
                register(object : River.PacketListener {
                    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
                        lagrePerson(dataSource, packet["fødselsnummer"].asText(), packet["aktørId"].asText(), packet["@opprettet"].asLocalDateTime())
                    }
                })
            }
        }

   GlobalScope.launch { seedApp.start() }
}

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
