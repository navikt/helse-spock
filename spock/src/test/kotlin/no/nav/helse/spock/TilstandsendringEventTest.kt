package no.nav.helse.spock

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class TilstandsendringEventTest {

    @Test
    internal fun `deserialize from json`() {
        assertNotNull(TilstandsendringEvent.fraJson(jsonWithUnknownFields))
    }

    @Test
    internal fun `addwhendue`(){
        val påminnelser = mutableListOf<TilstandsendringEvent>()

        assertFalse(tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now(), timeout = 60).addWhenDue(påminnelser))
        assertEquals(0, påminnelser.size)

        assertTrue(tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "B", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 60).addWhenDue(påminnelser))
        assertEquals(1, påminnelser.size)
    }

    @Test
    internal fun `nyeste`() {
        val event1 = tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1),timeout = 60)
        val event2 = tilstandsEndringsEvent(vedtaksPeriodeId = "2", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1),timeout = 60)
        val event3 = tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "B", endringstidspunkt = LocalDateTime.now(),timeout = 60)

        assertThrows<IllegalArgumentException>{
            event1.nyeste(event2)
        }

        assertEquals(event1,event1.nyeste(null))
        assertEquals(event3,event1.nyeste(event3))
        assertEquals(event3,event3.nyeste(event1))

    }
    private fun tilstandsEndringsEvent(vedtaksPeriodeId: String, tilstand: String, endringstidspunkt: LocalDateTime, timeout: Long) = requireNotNull(TilstandsendringEvent.fraJson("""
{
  "aktørId": "1234567890123",
  "fødselsnummer": "01019000000",
  "organisasjonsnummer": "123456789",
  "vedtaksperiodeId": "$vedtaksPeriodeId",
  "gjeldendeTilstand": "$tilstand",
  "forrigeTilstand": "START",
  "endringstidspunkt": "$endringstidspunkt",
  "timeout": $timeout
}"""))

    private val jsonWithUnknownFields = """
{
  "aktørId": "1234567890123",
  "fødselsnummer": "01019000000",
  "organisasjonsnummer": "123456789",
  "vedtaksperiodeId": "16f5801c-20b8-4471-8c7c-5e40f3d560f1",
  "gjeldendeTilstand": "NY_SØKNAD_MOTTATT",
  "forrigeTilstand": "START",
  "endringstidspunkt": "2019-01-01T00:00:00.000000",
  "timeout": 3600,
  "et_ukjent_felt": "en_ukjent_verdi"
}"""
}
