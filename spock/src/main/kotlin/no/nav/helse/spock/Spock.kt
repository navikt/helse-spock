package no.nav.helse.spock

import com.zaxxer.hikari.HikariConfig
import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.ApplicationStopping
import io.ktor.application.log
import io.ktor.util.KtorExperimentalAPI
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
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
import java.util.*
import javax.sql.DataSource

private const val rapidTopic = "helse-rapid-v1"

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

fun lagreTilstandsendring(dataSource: DataSource, event: TilstandsendringEventDto) {
    using(sessionOf(dataSource)) { session ->
        session.transaction {
            session.run(
                queryOf(
                    "INSERT INTO paminnelse (aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, timeout, endringstidspunkt, neste_paminnelsetidspunkt, data) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, (to_json(?::json)))",
                    event.aktørId,
                    event.fødselsnummer,
                    event.organisasjonsnummer,
                    event.vedtaksperiodeId,
                    event.tilstand,
                    event.timeout,
                    event.endringstidspunkt,
                    event.endringstidspunkt.plusSeconds(event.timeout),
                    event.originalJson
                ).asExecute
            )

            hentGjeldendeTilstand(session, event.vedtaksperiodeId)?.also { nyesteEndringsevent ->
                if (nyesteEndringsevent.timeout <= 0) {
                    session.run(queryOf("DELETE FROM paminnelse WHERE vedtaksperiode_id=?",
                        event.vedtaksperiodeId).asExecute)
                } else {
                    session.run(queryOf("DELETE FROM paminnelse WHERE vedtaksperiode_id=? AND id != ?::bigint",
                        event.vedtaksperiodeId, nyesteEndringsevent.id).asExecute)
                }
            }
        }
    }
}

private class GjeldeneTilstand(val id: String, val timeout: Long)
private fun hentGjeldendeTilstand(session: Session, vedtaksperiodeId: String): GjeldeneTilstand? {
    return session.run(
        queryOf("SELECT id, timeout FROM paminnelse " +
                "WHERE vedtaksperiode_id = ? " +
                "ORDER BY endringstidspunkt DESC, opprettet DESC " +
                "LIMIT 1", vedtaksperiodeId).map {
            GjeldeneTilstand(
                id = it.string(1),
                timeout = it.long(2)
            )
        }.asSingle
    )
}

fun hentPåminnelser(dataSource: DataSource): List<PåminnelseDto> {
    return using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT id, aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, timeout, endringstidspunkt, antall_ganger_paminnet, neste_paminnelsetidspunkt " +
                        "FROM paminnelse " +
                        "WHERE timeout > 0 AND neste_paminnelsetidspunkt <= now() " +
                        "AND id IN (SELECT DISTINCT ON (vedtaksperiode_id) id FROM paminnelse ORDER BY vedtaksperiode_id, endringstidspunkt DESC, opprettet DESC)"
            ).map {
                PåminnelseDto(
                    id = it.string("id"),
                    aktørId = it.string("aktor_id"),
                    fødselsnummer = it.string("fnr"),
                    organisasjonsnummer = it.string("organisasjonsnummer"),
                    vedtaksperiodeId = it.string("vedtaksperiode_id"),
                    tilstand = it.string("tilstand"),
                    timeout = it.long("timeout"),
                    endringstidspunkt = it.localDateTime("endringstidspunkt"),
                    antallGangerPåminnet = it.int("antall_ganger_paminnet") + 1
                )
            }.asList
        )
    }
}

fun oppdaterPåminnelse(dataSource: DataSource, påminnelse: PåminnelseDto) {
    using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE paminnelse SET neste_paminnelsetidspunkt = (now() + timeout * interval '1 second'), antall_ganger_paminnet = antall_ganger_paminnet + 1 WHERE id=?::BIGINT",
                påminnelse.id
            ).asExecute
        )
    }
}

@KtorExperimentalAPI
fun Application.spockApplication(): KafkaStreams {

    val secureLogger = LoggerFactory.getLogger("tjenestekall")

    migrate(createHikariConfigFromEnvironment())

    val dataSource = getDataSource(createHikariConfigFromEnvironment())

    val builder = StreamsBuilder()

    builder.stream<String, String>(
        listOf(rapidTopic), Consumed.with(Serdes.String(), Serdes.String())
            .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
    ).mapValues { _, json ->
        TilstandsendringEventDto.fraJson(json)
    }.mapNotNull()
        .peek { _, event -> lagreTilstandsendring(dataSource, event) }
        .flatMapValues { _, _ -> hentPåminnelser(dataSource) }
        .peek { _, påminnelse -> oppdaterPåminnelse(dataSource, påminnelse) }
        .mapValues { _, påminnelse -> påminnelse.toJson() }
        .peek { _, påminnelse -> secureLogger.info("Produserer $påminnelse") }
        .to(rapidTopic, Produced.with(Serdes.String(), Serdes.String()))

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
    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

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
