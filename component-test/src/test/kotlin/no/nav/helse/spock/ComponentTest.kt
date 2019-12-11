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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.sql.Connection
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

internal class ComponentTest {
    private companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"
        private const val kafkaApplicationId = "spock-v1"
        private const val påminnelserTopic = "privat-helse-sykepenger-paminnelser"
        private const val vedtaksperiodeEndretEventTopic = "privat-helse-sykepenger-vedtaksperiode-endret"

        private val topics = listOf(påminnelserTopic, vedtaksperiodeEndretEventTopic)
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
        fun `sender påminnelse`() {
            val vedtaksPeriodeId = "1"
            kafkaProducer.send(ProducerRecord(vedtaksperiodeEndretEventTopic, tilstandsEndringsEvent(vedtaksPeriodeId = vedtaksPeriodeId, tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 60)))

            await().atMost(30, TimeUnit.SECONDS).untilAsserted {
                assertEquals(1,
                        TestConsumer.records(påminnelserTopic)
                        .map { objectMapper.readTree(it.value()) }
                        .filter {it.hasNonNull("vedtaksperiodeId")}
                        .filter {it["vedtaksperiodeId"].textValue() == vedtaksPeriodeId}.size)
            }
        }

        private fun tilstandsEndringsEvent(vedtaksPeriodeId: String, tilstand: String, endringstidspunkt: LocalDateTime, timeout: Long) = """
{
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
