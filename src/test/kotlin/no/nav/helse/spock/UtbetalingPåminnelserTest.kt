package no.nav.helse.spock

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spock.UtbetalingPåminnelser.Utbetalingpåminnelse.Companion.nestePåminnelsetidspunkt
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtbetalingPåminnelserTest {
    private val logCollector = ListAppender<ILoggingEvent>()

    init {
        (LoggerFactory.getLogger("tjenestekall") as Logger).addAppender(logCollector)
        logCollector.start()
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var rapid: TestRapid
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun `start postgres`() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        val dsbuilder = DataSourceBuilder(
            mapOf(
                "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
            )
        )

        dsbuilder.migrate()
        dataSource = dsbuilder.getDataSource()
    }

    @AfterAll
    fun `stop postgres`() {
        embeddedPostgres.close()
    }

    @BeforeEach
    fun setup() {
        logCollector.list.clear()
        rapid = TestRapid().apply {
            UtbetalingEndret(this, dataSource)
            UtbetalingPåminnelser(this, dataSource, Duration.ofMillis(1))
        }
    }

    @Test
    fun `out of order`() {
        val utbetalingId = UUID.randomUUID()
        val status = "SENDT"
        val messageEldst = utbetalingEndret(utbetalingId, "GODKJENT")
        val messageNyest = utbetalingEndret(utbetalingId, status)
        rapid.sendTestMessage(messageNyest)
        rapid.sendTestMessage(messageEldst)
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `overskriver status 1`() {
        val utbetalingId = UUID.randomUUID()
        val status = "SENDT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, "GODKJENT"))
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `overskriver status 2`() {
        val utbetalingId = UUID.randomUUID()
        val status = "UTBETALT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, "GODKJENT"))
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, "SENDT"))
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertIngenPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager påminnelse for godkjent`() {
        val utbetalingId = UUID.randomUUID()
        val status = "GODKJENT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager påminnelse for sendt`() {
        val utbetalingId = UUID.randomUUID()
        val status = "SENDT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager påminnelse for overført`() {
        val utbetalingId = UUID.randomUUID()
        val status = "OVERFØRT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager ikke påminnelse for utbetaling feilet`() {
        val utbetalingId = UUID.randomUUID()
        val status = "UTBETALING_FEILET"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertIngenPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager ikke påminnelse for utbetalt`() {
        val utbetalingId = UUID.randomUUID()
        val status = "UTBETALT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertIngenPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager ikke påminnelse for annullert`() {
        val utbetalingId = UUID.randomUUID()
        val status = "ANNULLERT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertIngenPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager ikke påminnelse for feriepenger med status UTBETALT`() {
        val utbetalingId = UUID.randomUUID()
        val type = "FERIEPENGER"
        val status = "UTBETALT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status, type))
        assertIngenPåminnelse(utbetalingId, status)
        assertFalse(logCollector.list.any { it.message.startsWith("kunne ikke forstå utbetaling_endret") })
    }

    @Test
    fun `kan lagre utbetalinger av type revurdering`() {
        val utbetalingId = UUID.randomUUID()
        val type = "REVURDERING"
        val status = "UTBETALT"
        assertDoesNotThrow {
            rapid.sendTestMessage(utbetalingEndret(utbetalingId, status = status, type = type))
        }
    }

    private fun assertPåminnelse(utbetalingId: UUID, status: String) {
        val meldinger = (0 until rapid.inspektør.size).map { rapid.inspektør.message(it) }
        assertTrue(meldinger.any {
            it.path("@event_name").asText() == "utbetalingpåminnelse"
                    && it.path("utbetalingId").asText() == utbetalingId.toString()
                    && it.path("status").asText() == status
        }) {
            "Fant ingen påminnelse: $meldinger"
        }
    }

    private fun assertIngenPåminnelse(utbetalingId: UUID, status: String) {
        val meldinger = (0 until rapid.inspektør.size).map { rapid.inspektør.message(it) }
        assertTrue(meldinger.none {
            it.path("@event_name").asText() == "utbetalingpåminnelse"
                    && it.path("utbetalingId").asText() == utbetalingId.toString()
                    && it.path("status").asText() == status
        }) {
            "Fant ingen påminnelse: $meldinger"
        }
    }

    private fun utbetalingEndret(
        utbetalingId: UUID,
        status: String,
        type: String = "UTBETALING"
    ): String {
        val now = LocalDateTime.now()
        val nestePåminnelsetidspunkt = nestePåminnelsetidspunkt(now, status)
        val endringstidspunkt = nestePåminnelsetidspunkt?.let {
            now.minusSeconds(ChronoUnit.SECONDS.between(now, nestePåminnelsetidspunkt))
        } ?: now

        return JsonMessage.newMessage(
            mapOf(
                "@event_name" to "utbetaling_endret",
                "@opprettet" to "$endringstidspunkt",
                "aktørId" to "1234567890123",
                "fødselsnummer" to "01019000000",
                "organisasjonsnummer" to "123456789",
                "utbetalingId" to utbetalingId.toString(),
                "type" to type,
                "gjeldendeStatus" to status,
                "forrigeStatus" to "IKKE_UTBETALT"
            )
        ).toJson()
    }
}
