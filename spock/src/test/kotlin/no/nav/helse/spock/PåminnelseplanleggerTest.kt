package no.nav.helse.spock

import no.nav.helse.spock.Tilstandsendringer.TilstandsendringEventDto.Companion.defaultIntervall
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

internal class PåminnelseplanleggerTest {
    private companion object {
        val enDag = LocalDate.of(2018, 1, 1)
    }

    @Test
    fun `regner ut neste påminnelsetidspunkt`() {
        repeat(100) {
            val t = defaultIntervall(enDag.atTime(4, 0))
            assertTrue(t >= enDag.atTime(18, 0, 0))
            assertTrue(t <= enDag.plusDays(2).atTime(5, 59, 0))
        }
        repeat(100) {
            val t = defaultIntervall(enDag.atTime(12, 0,0))
            assertTrue(t >= enDag.plusDays(1).atTime(18, 0, 0))
            assertTrue(t <= enDag.plusDays(2).atTime(5, 59, 0))
        }
        repeat(100) {
            val t = defaultIntervall(enDag.atTime(23, 59,0))
            assertTrue(t >= enDag.plusDays(1).atTime(18, 0, 0))
            assertTrue(t <= enDag.plusDays(2).atTime(5, 59, 0))
        }
    }
}