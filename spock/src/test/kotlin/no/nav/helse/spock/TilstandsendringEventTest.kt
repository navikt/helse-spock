package no.nav.helse.spock

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TilstandsendringEventTest {

    @Test
    internal fun `deserialize from json`() {
        assertNotNull(TilstandsendringEvent.fraJson(jsonWithUnknownFields))
    }

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
