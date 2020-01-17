package no.nav.helse.spock

import com.zaxxer.hikari.HikariConfig
import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.ApplicationStopping
import io.ktor.application.log
import io.ktor.util.KtorExperimentalAPI
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.util.Properties

private const val vedtaksperiodeEndretEventTopic = "privat-helse-sykepenger-vedtaksperiode-endret"
private const val påminnelserTopic = "privat-helse-sykepenger-paminnelser"

fun createHikariConfig(jdbcUrl: String, username: String? = null, password: String? = null) =
    HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        username?.let { this.username = it }
        password?.let { this.password = it }
    }

@KtorExperimentalAPI
fun Application.createHikariConfigFromEnvironment() =
    createHikariConfig(
        jdbcUrl = environment.config.property("database.jdbc-url").getString(),
        username = environment.config.propertyOrNull("database.username")?.getString(),
        password = environment.config.propertyOrNull("database.password")?.getString()
    )

@KtorExperimentalAPI
fun Application.spockApplication(): KafkaStreams {

    val secureLogger = LoggerFactory.getLogger("tjenestekall")

    migrate(createHikariConfigFromEnvironment())

    val dataSource = getDataSource(createHikariConfigFromEnvironment()) // todo: ta i bruk

    val builder = StreamsBuilder()

    val påminnelser = Påminnelser()

    builder.stream<String, String>(
        listOf(vedtaksperiodeEndretEventTopic), Consumed.with(Serdes.String(), Serdes.String())
            .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
    ).mapValues { _, json -> TilstandsendringEvent.fraJson(json) }
        .mapNotNull()
        .flatMapValues { _, event ->
            påminnelser.håndter(event)
        }.peek { _, påminnelse ->
            log.info("Produserer påminnelse: ${påminnelse.infoLogg()}")
        }
        .mapValues { _, påminnelse ->
            påminnelse.toJson()
        }.peek { _, påminnelse ->
            secureLogger.info("Produserer $påminnelse")
        }.to(påminnelserTopic, Produced.with(Serdes.String(), Serdes.String()))

    return KafkaStreams(builder.build(), streamsConfig()).apply {
        addShutdownHook(this)

        environment.monitor.subscribe(ApplicationStarted) {
            start()
        }

        environment.monitor.subscribe(ApplicationStopping) {
            close(Duration.ofSeconds(10))
        }
    }
}

private fun <Key : Any, Value : Any> KStream<Key, Value?>.mapNotNull() =
    filter { _, value ->
        value != null
    }.mapValues { _, value ->
        value!!
    }

@KtorExperimentalAPI
private fun Application.streamsConfig() = Properties().apply {
    put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, environment.config.property("kafka.bootstrap-servers").getString())
    put(StreamsConfig.APPLICATION_ID_CONFIG, environment.config.property("kafka.app-id").getString())

    put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)

    put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")

    environment.config.propertyOrNull("serviceuser.username")?.getString()?.let { username ->
        environment.config.propertyOrNull("serviceuser.password")?.getString()?.let { password ->
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
            )
        }
    }

    environment.config.propertyOrNull("kafka.truststore-path")?.getString()?.let { truststorePath ->
        environment.config.propertyOrNull("kafka.truststore-password")?.getString().let { truststorePassword ->
            try {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(truststorePath).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
                log.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
            } catch (ex: Exception) {
                log.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
            }
        }
    }
}

private fun Application.addShutdownHook(streams: KafkaStreams) {
    streams.setStateListener { newState, oldState ->
        log.info("From state={} to state={}", oldState, newState)

        if (newState == KafkaStreams.State.ERROR) {
            // if the stream has died there is no reason to keep spinning
            log.warn("No reason to keep living, closing stream")
            streams.close(Duration.ofSeconds(10))
        }
    }
    streams.setUncaughtExceptionHandler { _, ex ->
        log.error("Caught exception in stream, exiting", ex)
        streams.close(Duration.ofSeconds(10))
    }
}
