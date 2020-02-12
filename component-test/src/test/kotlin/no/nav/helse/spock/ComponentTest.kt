package no.nav.helse.spock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.common.KafkaEnvironment
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.sql.Connection
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.collections.HashMap

internal class ComponentTest {
    private companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"
        private const val kafkaApplicationId = "spock-v1"
        private const val rapidTopic = "helse-rapid-v1"

        private val topics = listOf(rapidTopic)
        // Use one partition per topic to make message sending more predictable
        private val topicInfos = topics.map { KafkaEnvironment.TopicInfo(it, partitions = 1) }

        private val embeddedKafkaEnvironment = KafkaEnvironment(
                autoStart = false,
                noOfBrokers = 1,
                topicInfos = topicInfos,
                withSchemaRegistry = false,
                withSecurity = false,
                topicNames = topics
        )

        private lateinit var adminClient: AdminClient
        private lateinit var kafkaProducer: KafkaProducer<String, String>

        private lateinit var embeddedPostgres: EmbeddedPostgres
        private lateinit var postgresConnection: Connection

        private lateinit var hikariConfig: HikariConfig

        private lateinit var embeddedServer: ApplicationEngine

        private fun applicationConfig(): Map<String, String> {
            return mapOf(
                    "KAFKA_APP_ID" to kafkaApplicationId,
                    "KAFKA_BOOTSTRAP_SERVERS" to embeddedKafkaEnvironment.brokersURL,
                    "SERVICEUSER_USERNAME" to username,
                    "SERVICEUSER_PASSWORD" to password,
                    "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres"),
                    "KAFKA_COMMIT_INTERVAL_MS_CONFIG" to "100"
            )
        }

        private fun producerProperties() =
                Properties().apply {
                    put(BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
                    put(SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                    // Make sure our producer waits until the message is received by Kafka before returning. This is to make sure the tests can send messages in a specific order
                    put(ACKS_CONFIG, "all")
                    put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
                    put(LINGER_MS_CONFIG, "0")
                    put(RETRIES_CONFIG, "0")
                    put(SASL_MECHANISM, "PLAIN")
                }

        private fun consumerProperties(): MutableMap<String, Any>? {
            return HashMap<String, Any>().apply {
                put(BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
                put(SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                put(SASL_MECHANISM, "PLAIN")
                put(GROUP_ID_CONFIG, "spockComponentTest")
                put(AUTO_OFFSET_RESET_CONFIG, "earliest")
            }
        }

        @KtorExperimentalAPI
        @BeforeAll
        @JvmStatic
        internal fun `start embedded environment`() {
            embeddedPostgres = EmbeddedPostgres.builder().start()
            postgresConnection = embeddedPostgres.postgresDatabase.connection
            hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
            runMigration(HikariDataSource(hikariConfig))

            embeddedKafkaEnvironment.start()
            adminClient = embeddedKafkaEnvironment.adminClient ?: fail("Klarte ikke få tak i adminclient")
            kafkaProducer = KafkaProducer(producerProperties(), StringSerializer(), StringSerializer())

            embeddedServer = embeddedServer(Netty, createApplicationEnvironment(createConfigFromEnvironment(applicationConfig())))
                    .start(wait = false)
        }

        @AfterAll
        @JvmStatic
        internal fun `stop embedded environment`() {
            adminClient.close()
            embeddedKafkaEnvironment.tearDown()
        }
    }

    @Test
    fun `sjekker at påminnelse blir sendt etter angitt timeout`() {
        val timeout = 1L
        var tilstand = "A"
        var forventetPåminnetEtter = LocalDateTime.now().plusSeconds(timeout)
        val vedtaksperiodeId = sendTilstandsendringEvent(
                timeout = timeout,
                tilstand = tilstand
        )

        var påminnelsenummer = 1
        forventetPåminnetEtter = ventPåMottattPåminnelse(vedtaksperiodeId, forventetPåminnetEtter, tilstand, påminnelsenummer)

        påminnelsenummer = 2
        forventetPåminnetEtter = ventPåMottattPåminnelse(vedtaksperiodeId, forventetPåminnetEtter, tilstand, påminnelsenummer)

        tilstand = "B"
        sendTilstandsendringEvent(
                vedtaksperiodeId = vedtaksperiodeId,
                tilstand = tilstand,
                timeout = timeout
        )

        påminnelsenummer = 1
        ventPåMottattPåminnelse(vedtaksperiodeId, forventetPåminnetEtter, tilstand, påminnelsenummer)

    }

    private fun ventPåMottattPåminnelse(vedtaksperiodeId: String, påminnelsetidspunkt: LocalDateTime, tilstand: String, påminnelsenummer: Int): LocalDateTime {
        return await("vent på påminnelse=$påminnelsenummer sendt etter=$påminnelsetidspunkt for vedtaksperiode=$vedtaksperiodeId i tilstand=$tilstand")
                .atMost(10, SECONDS)
                .until (mottattPåminnelse(vedtaksperiodeId, tilstand, påminnelsenummer)) {
                    it >= påminnelsetidspunkt
                }
    }

    private fun mottattPåminnelse(vedtaksperiodeId: String, tilstand: String, påminnelsenummer: Int): () -> LocalDateTime = {
        // send flere meldinger for å sørge for litt trafikk
        sendTilstandsendringEvent(timeout = 3600)

        TestConsumer.records(rapidTopic)
                .map { it.value() }
                .also { println("read $it") }
                .map { objectMapper.readTree(it) }
                .filter { it.hasNonNull("vedtaksperiodeId") }
                .filter { it.hasNonNull("tilstand") }
                .filter { it.hasNonNull("antallGangerPåminnet") }
                .filter { it.hasNonNull("påminnelsestidspunkt") }
                .filter { it["vedtaksperiodeId"].textValue() == vedtaksperiodeId }
                .filter { it["tilstand"].textValue() == tilstand }
                .firstOrNull { it["antallGangerPåminnet"].intValue() == påminnelsenummer }
                ?.let { LocalDateTime.parse(it["påminnelsestidspunkt"].textValue()) } ?: LocalDateTime.MIN
    }

    private fun sendTilstandsendringEvent(
            vedtaksperiodeId: String = UUID.randomUUID().toString(),
            tilstand: String = UUID.randomUUID().toString(),
            endringstidspunkt: LocalDateTime = LocalDateTime.now(),
            timeout: Long
    ): String {
        kafkaProducer.send(ProducerRecord(rapidTopic, tilstandsEndringsEvent(
                vedtaksPeriodeId = vedtaksperiodeId,
                tilstand = tilstand,
                endringstidspunkt = endringstidspunkt,
                timeout = timeout
        ).also { println("sender $it")})).get()
        return vedtaksperiodeId
    }

    private fun tilstandsEndringsEvent(vedtaksPeriodeId: String, tilstand: String, endringstidspunkt: LocalDateTime, timeout: Long) = """
{
  "@event_name": "vedtaksperiode_endret",
  "aktørId": "1234567890123",
  "fødselsnummer": "01019000000",
  "organisasjonsnummer": "123456789",
  "vedtaksperiodeId": "$vedtaksPeriodeId",
  "gjeldendeTilstand": "$tilstand",
  "forrigeTilstand": "START",
  "endringstidspunkt": "$endringstidspunkt",
  "timeout": $timeout
}"""

    private object TestConsumer {
        private val records = mutableListOf<ConsumerRecord<String, String>>()

        private val kafkaConsumer =
                KafkaConsumer(consumerProperties(), StringDeserializer(), StringDeserializer()).also {
                    it.subscribe(topics)
                }

        fun reset() {
            records.clear()
        }

        fun records(topic: String) = records().filter { it.topic() == topic }

        fun records() =
                records.also { it.addAll(kafkaConsumer.poll(Duration.ofMillis(0))) }

        fun close() {
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close()
        }
    }
}
