package no.nav.helse.spock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PåminnelserTest {
    @Test
    internal fun `håndter`() {
        val påminnelser = Påminnelser()

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 60).also {
            assertEquals(1, påminnelser.håndter(it).size)
        }

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "B", endringstidspunkt = LocalDateTime.now(), timeout = 60).also {
            assertTrue(påminnelser.håndter(it).isEmpty())
        }
    }

    @Test
    internal fun `håndter ulike vedtaksperioder`() {
        val påminnelser = Påminnelser()

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 60).also {
            assertEquals(1, påminnelser.håndter(it).size)
        }

        tilstandsEndringsEvent(vedtaksPeriodeId = "2", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 60).also {
            assertEquals(1, påminnelser.håndter(it).size)
        }

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "B", endringstidspunkt = LocalDateTime.now(), timeout = 60).also {
            assertTrue(påminnelser.håndter(it).isEmpty())
        }
    }

    @Test
    internal fun `håndter timeout 0`() {
        val påminnelser = Påminnelser()

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 0).also {
            assertTrue(påminnelser.håndter(it).isEmpty())
        }
    }

    @Test
    internal fun `håndter timeout 0 med en påminnelse fra før`() {
        val påminnelser = Påminnelser()

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "B", endringstidspunkt = LocalDateTime.now().minusMinutes(2), timeout = 60).also {
            assertEquals(1, påminnelser.håndter(it).size)
        }

        tilstandsEndringsEvent(vedtaksPeriodeId = "1", tilstand = "A", endringstidspunkt = LocalDateTime.now().minusHours(1), timeout = 0).also {
            assertTrue(påminnelser.håndter(it).isEmpty())
        }
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
