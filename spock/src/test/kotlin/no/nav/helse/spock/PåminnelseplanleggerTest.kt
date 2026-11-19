package no.nav.helse.spock

import no.nav.helse.spock.Tilstandsendringer.TilstandsendringEventDto.Companion.defaultIntervall
import no.nav.helse.spock.Tilstandsendringer.TilstandsendringEventDto.Companion.nestePåminnelsetidspunkt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

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
            val t = defaultIntervall(enDag.atTime(12, 0, 0))
            assertTrue(t >= enDag.plusDays(1).atTime(18, 0, 0))
            assertTrue(t <= enDag.plusDays(2).atTime(5, 59, 0))
        }
        repeat(100) {
            val t = defaultIntervall(enDag.atTime(23, 59, 0))
            assertTrue(t >= enDag.plusDays(1).atTime(18, 0, 0))
            assertTrue(t <= enDag.plusDays(2).atTime(5, 59, 0))
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "AVVENTER_RETTIGHETSKATEGORI",
            "AVVENTER_SYKEPENGEGRUNNLAG",
            "AVVENTER_SYKEPENGEGRUNNLAG_REVURDERING",
        ],
    )
    fun `påminner etter én time`(tilstand: String) {
        val endringstidspunkt = enDag.atTime(12, 0)
        assertEquals(
            endringstidspunkt.plusHours(1),
            nestePåminnelsetidspunkt(tilstand, endringstidspunkt, 0),
        )
    }
}
