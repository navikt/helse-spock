package no.nav.helse.spock

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpleisPåminnelserTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var rapid: TestRapid

    @BeforeAll
    fun `start postgres`() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        val dsbuilder = DataSourceBuilder(mapOf(
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        ))

        dsbuilder.migrate()
        val dataSource = dsbuilder.getDataSource()

        rapid =  TestRapid().apply {
            Tilstandsendringer(this, dataSource)
            Påminnelser(this, dataSource, SpesialistPåminnelseDao(dataSource), Duration.ofMillis(1))
        }
    }

    @AfterAll
    fun `stop postgres`() {
        embeddedPostgres.close()
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
    }

    @Test
    fun `lager påminnelser`() {
        val vedtaksperiodeId = UUID.randomUUID().toString()
        val tilstand = "AVVENTER_GAP"
        val endringstidspunkt = LocalDateTime
            .now()
            .minusSeconds(
                ChronoUnit.SECONDS.between(
                    LocalDateTime.now(),
                    Tilstandsendringer.TilstandsendringEventDto.nestePåminnelsetidspunkt(tilstand, LocalDateTime.now(), 0)
                )
            )
            .plusSeconds(5)
        rapid.sendTestMessage(tilstandsendringsevent(vedtaksperiodeId, tilstand, endringstidspunkt))
        await("skal produsere påminnelse")
            .atMost(10, TimeUnit.SECONDS)
            .until {
                rapid.sendTestMessage("{}") // create noise on the rapid

                val inspektør = rapid.inspektør
                if (inspektør.size == 0) false
                else (0 until inspektør.size)
                    .any {
                        inspektør.field(it, "@event_name").asText() == "påminnelse"
                                && inspektør.field(it, "vedtaksperiodeId").asText() == vedtaksperiodeId
                                && inspektør.field(it, "tilstand").asText() == tilstand
                    }
            }
    }

    private fun tilstandsendringsevent(
        vedtaksperiodeId: String,
        tilstand: String,
        endringstidspunkt: LocalDateTime
    ) = JsonMessage.newMessage(mapOf(
        "@event_name" to "vedtaksperiode_endret",
        "aktørId" to "1234567890123",
        "fødselsnummer" to "01019000000",
        "organisasjonsnummer" to "123456789",
        "vedtaksperiodeId" to vedtaksperiodeId,
        "gjeldendeTilstand" to tilstand,
        "forrigeTilstand" to "START",
        "@opprettet" to "$endringstidspunkt"
    )).toJson()

    private fun createHikariConfig(jdbcUrl: String, username: String? = null, password: String? = null) =
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
}
