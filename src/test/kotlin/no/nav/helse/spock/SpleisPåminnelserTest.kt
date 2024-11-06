package no.nav.helse.spock

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.test_support.TestDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource

internal class SpleisPåminnelserTest {

    private lateinit var rapid: TestRapid
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        rapid = TestRapid().apply {
            Forkastelser(this, dataSource.ds)
            Tilstandsendringer(this, dataSource.ds)
            IkkePåminnelser(this, dataSource.ds)
            Påminnelser(this, dataSource.ds)
            PersonAvstemminger(this, dataSource.ds)
        }
    }

    @AfterEach
    fun teardown() {
        databaseContainer.droppTilkobling(dataSource)
    }

    @Test
    fun `lager påminnelser`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val tilstand = "AVVENTER_HISTORIKK"
        val now = LocalDateTime.now()
        val nestePåminnelsetidspunkt = Tilstandsendringer.TilstandsendringEventDto.nestePåminnelsetidspunkt(tilstand, now, 0)
        val endringstidspunkt = now
            .minusSeconds(ChronoUnit.SECONDS.between(now, nestePåminnelsetidspunkt))
            .minusSeconds(1)
        rapid.sendTestMessage(tilstandsendringsevent(vedtaksperiodeId, tilstand, endringstidspunkt))
        rapid.sendTestMessage(kjørSpock())
        val påminnelse = rapid.inspektør.message(rapid.inspektør.size - 1)
        assertEquals("påminnelse", påminnelse.path("@event_name").asText())
        assertEquals(vedtaksperiodeId.toString(), påminnelse.path("vedtaksperiodeId").asText())
        assertEquals(tilstand, påminnelse.path("tilstand").asText())
    }

    @Test
    fun `sletter påminnelser når en vedtaksperiode blir slettet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        rapid.sendTestMessage(
            tilstandsendringsevent(
                vedtaksperiodeId,
                "AVVENTER_INNTEKTSMELDING_FERDIG_GAP",
                LocalDate.EPOCH.atStartOfDay()
            )
        )
        assertEquals(1, hentAntallPåminnelser(vedtaksperiodeId))

        rapid.sendTestMessage(vedtaksperiodeForkastet(vedtaksperiodeId))
        assertEquals(0, hentAntallPåminnelser(vedtaksperiodeId))
    }

    @Test
    fun `retter tilstand når påminnelse var uaktuell`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val tilstand = "AVVENTER_SØKNAD_UFERDIG_GAP"
        val endringstidspunkt = LocalDateTime
            .now()
            .minusHours(24)
        val nyttTidspunkt = LocalDateTime.now()
        rapid.sendTestMessage(tilstandsendringsevent(vedtaksperiodeId, tilstand, endringstidspunkt))
        rapid.sendTestMessage(ikkePåminnelseEvent(vedtaksperiodeId, "TIL_UTBETALING", nyttTidspunkt))

        val påminnelse = hentPåminnelseFraDatabasen(dataSource.ds, vedtaksperiodeId)

        assertEquals(nyttTidspunkt, påminnelse.endringstidspunkt)
        assertEquals("TIL_UTBETALING", påminnelse.tilstand)
    }

    @Test
    fun `fikser tilstand fra avstemmingsresultat`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val tilstand = "AVVENTER_SØKNAD_UFERDIG_GAP"
        val endringstidspunkt = LocalDateTime
            .now()
            .minusHours(24)
        val nyttTidspunkt = LocalDateTime.now()
        rapid.sendTestMessage(tilstandsendringsevent(vedtaksperiodeId, tilstand, endringstidspunkt))
        rapid.sendTestMessage(avstemming(vedtaksperiodeId, "TIL_UTBETALING", nyttTidspunkt))

        val påminnelse = hentPåminnelseFraDatabasen(dataSource.ds, vedtaksperiodeId)

        assertEquals(nyttTidspunkt, påminnelse.endringstidspunkt)
        assertEquals("TIL_UTBETALING", påminnelse.tilstand)
    }

    @Test
    fun `Endrer ikke påminnelse dersom event om uaktuell påminnelse er eldre enn nåværende`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val tilstand = "AVVENTER_SØKNAD_UFERDIG_GAP"
        val endringstidspunkt = LocalDateTime.now()
        val nyttTidspunkt = LocalDateTime.now().minusHours(24)
        rapid.sendTestMessage(tilstandsendringsevent(vedtaksperiodeId, tilstand, endringstidspunkt))
        rapid.sendTestMessage(ikkePåminnelseEvent(vedtaksperiodeId, "TIL_UTBETALING", nyttTidspunkt))

        val påminnelse = hentPåminnelseFraDatabasen(dataSource.ds, vedtaksperiodeId)
        assertEquals(endringstidspunkt, påminnelse.endringstidspunkt)
        assertEquals(tilstand, påminnelse.tilstand)
    }

    private fun hentAntallPåminnelser(vedtaksperiodeId: UUID) = sessionOf(dataSource.ds).use { session ->
        session.run(queryOf(
            "SELECT count(*) as vedtaksperiode_count FROM paminnelse WHERE vedtaksperiode_id=?;",
            vedtaksperiodeId.toString()
        )
            .map { it.long("vedtaksperiode_count") }
            .asSingle)
    }

    @Language("JSON")
    private fun vedtaksperiodeForkastet(vedtaksperiodeId: UUID) = """{
            "@event_name": "vedtaksperiode_forkastet",
            "hendelseId": "030001BD-8FBA-4324-9725-D618CE5B83E9",
            "vedtaksperiodeId": "$vedtaksperiodeId"
        }"""

    private fun kjørSpock() = JsonMessage.newMessage("kjør_spock").toJson()

    private fun tilstandsendringsevent(
        vedtaksperiodeId: UUID,
        tilstand: String,
        endringstidspunkt: LocalDateTime
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "vedtaksperiode_endret",
            "aktørId" to "1234567890123",
            "fødselsnummer" to "01019000000",
            "organisasjonsnummer" to "123456789",
            "vedtaksperiodeId" to vedtaksperiodeId.toString(),
            "gjeldendeTilstand" to tilstand,
            "forrigeTilstand" to "START",
            "@opprettet" to "$endringstidspunkt"
        )
    ).toJson()

    private fun avstemming(
        vedtaksperiodeId: UUID,
        tilstand: String,
        endringstidspunkt: LocalDateTime
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "person_avstemt",
            "@id" to UUID.randomUUID().toString(),
            "aktørId" to "1234567890123",
            "fødselsnummer" to "01019000000",
            "arbeidsgivere" to listOf(
                mapOf(
                    "organisasjonsnummer" to "123456789",
                    "vedtaksperioder" to listOf(
                        mapOf(
                            "id" to vedtaksperiodeId.toString(),
                            "tilstand" to tilstand,
                            "opprettet" to "${endringstidspunkt.minusDays(1)}",
                            "oppdatert" to "$endringstidspunkt"
                        )
                    ),
                    "forkastedeVedtaksperioder" to emptyList<Map<String, Any>>(),
                    "utbetalinger" to emptyList<Map<String, Any>>()

                )
            ),
            "@opprettet" to "$endringstidspunkt"
        )
    ).toJson()

    private fun ikkePåminnelseEvent(
        vedtaksperiodeId: UUID,
        tilstand: String,
        endringstidspunkt: LocalDateTime
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "vedtaksperiode_ikke_påminnet",
            "aktørId" to "1234567890123",
            "fødselsnummer" to "01019000000",
            "organisasjonsnummer" to "123456789",
            "vedtaksperiodeId" to vedtaksperiodeId.toString(),
            "tilstand" to tilstand,
            "@opprettet" to "$endringstidspunkt"
        )
    ).toJson()

    private fun hentPåminnelseFraDatabasen(dataSource: DataSource, vedtaksperiodeId: UUID): PåminnelseDto {
        return requireNotNull(sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "SELECT id, aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, endringstidspunkt, endringstidspunkt_nanos, antall_ganger_paminnet, neste_paminnelsetidspunkt " +
                                "FROM paminnelse WHERE vedtaksperiode_id = ?", vedtaksperiodeId.toString()
                    ).map {
                        PåminnelseDto(
                            id = it.string("id"),
                            aktørId = it.string("aktor_id"),
                            fødselsnummer = it.string("fnr"),
                            organisasjonsnummer = it.string("organisasjonsnummer"),
                            vedtaksperiodeId = it.string("vedtaksperiode_id"),
                            tilstand = it.string("tilstand"),
                            endringstidspunkt = it.localDateTime("endringstidspunkt")
                                .withNano(it.int("endringstidspunkt_nanos")),
                            antallGangerPåminnet = it.int("antall_ganger_paminnet") + 1
                        )
                    }.asSingle
                )
            }
        }) { "Fant ikke påminnelse for vedtaksperiodeId=$vedtaksperiodeId" }
    }
}
