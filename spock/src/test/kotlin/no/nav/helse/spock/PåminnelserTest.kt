package no.nav.helse.spock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PåminnelserTest {
    @Test
    internal fun `håndter`() {
        val påminnelser = Påminnelser()

        assertEquals(0, påminnelser.påminnelser().size)

        val event1 = tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 60)
        påminnelser.håndter(event1)
        assertEquals(1, påminnelser.påminnelser().size)

        val event2 = tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "B", endringstidspunkt = LocalDateTime.now(), timeout = 60)
        påminnelser.håndter(event2)
        assertEquals(0, påminnelser.påminnelser().size)
    }

    @Test
    internal fun `håndter timeout 0`() {
        val påminnelser = Påminnelser()

        val event1 = tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 0)
        påminnelser.håndter(event1)
        assertEquals(0, påminnelser.påminnelser().size)
    }

    @Test
    internal fun `håndter timeout 0 med en påminnelse fra før`() {
        val påminnelser = Påminnelser()

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "B", endringstidspunkt = LocalDateTime.now().minusMinutes(2), timeout = 60).also {
            påminnelser.håndter(it)
        }

        val event1 = tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 0)
        påminnelser.håndter(event1)

        assertEquals(1, påminnelser.påminnelser().size)
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
}
