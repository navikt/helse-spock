package no.nav.helse.spock

import com.github.navikt.tbd_libs.test_support.TestDataSource
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spock.UtbetalingPåminnelser.Utbetalingpåminnelse.Companion.nestePåminnelsetidspunkt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class UtbetalingPåminnelserTest {
    private lateinit var rapid: TestRapid
    private lateinit var dataSource: TestDataSource
    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()

        rapid = TestRapid().apply {
            UtbetalingEndret(this, dataSource.ds)
            UtbetalingPåminnelser(this, dataSource.ds)
        }
    }

    @AfterEach
    fun teardown() {
        databaseContainer.droppTilkobling(dataSource)
    }

    @Test
    fun `out of order`() {
        val utbetalingId = UUID.randomUUID()
        val status = "AVVENTER_PERSONKVITTERING"
        val messageEldst = utbetalingEndret(utbetalingId, "AVVENTER_KVITTERINGER")
        val messageNyest = utbetalingEndret(utbetalingId, status)
        rapid.sendTestMessage(messageNyest)
        rapid.sendTestMessage(messageEldst)
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `overskriver status 1`() {
        val utbetalingId = UUID.randomUUID()
        val status = "AVVENTER_PERSONKVITTERING"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, "AVVENTER_KVITTERINGER"))
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `overskriver status 2`() {
        val utbetalingId = UUID.randomUUID()
        val status = "UTBETALT"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, "AVVENTER_KVITTERINGER"))
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, "AVVENTER_ARBEIDSGIVERKVITTERING"))
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertIngenPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager påminnelse for AVVENTER_KVITTERINGER`() {
        val utbetalingId = UUID.randomUUID()
        val status = "AVVENTER_KVITTERINGER"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager påminnelse for AVVENTER_ARBEIDSGIVERKVITTERING`() {
        val utbetalingId = UUID.randomUUID()
        val status = "AVVENTER_ARBEIDSGIVERKVITTERING"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
    }

    @Test
    fun `lager påminnelse for AVVENTER_PERSONKVITTERING`() {
        val utbetalingId = UUID.randomUUID()
        val status = "AVVENTER_PERSONKVITTERING"
        rapid.sendTestMessage(utbetalingEndret(utbetalingId, status))
        assertPåminnelse(utbetalingId, status)
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
        rapid.sendTestMessage(kjørSpock())
        val sisteMelding = rapid.inspektør.message(rapid.inspektør.size - 1)
        assertEquals("utbetalingpåminnelse", sisteMelding.path("@event_name").asText())
        assertEquals(utbetalingId.toString(), sisteMelding.path("utbetalingId").asText())
        assertEquals(status, sisteMelding.path("status").asText())
    }

    private fun kjørSpock() = JsonMessage.newMessage("kjør_spock").toJson()

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
            now.minusSeconds(60 + ChronoUnit.SECONDS.between(now, nestePåminnelsetidspunkt))
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
