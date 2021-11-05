package no.nav.helse.spock

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.awaitility.Awaitility.await
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpleisPåminnelserTest {

    private lateinit var rapid: TestRapid
    private lateinit var dataSource: DataSource
    private val postgres = PostgreSQLContainer<Nothing>("postgres:13")

    @BeforeAll
    fun `start postgres`() {
        postgres.start()
        val dsbuilder = DataSourceBuilder(
            mapOf(
                "DATABASE_JDBC_URL" to postgres.jdbcUrl,
                "DATABASE_USERNAME" to postgres.username,
                "DATABASE_PASSWORD" to postgres.password,
            )
        )

        dsbuilder.migrate()
        dataSource = dsbuilder.getDataSource()

        rapid = TestRapid().apply {
            Forkastelser(this, dataSource)
            Tilstandsendringer(this, dataSource)
            IkkePåminnelser(this, dataSource)
            Påminnelser(this, dataSource, Duration.ofMillis(1))
        }
    }

    @AfterAll
    fun `stop postgres`() {
        postgres.stop()
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
    }

    @Test
    fun `lager påminnelser`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val tilstand = "AVVENTER_HISTORIKK"
        val endringstidspunkt = LocalDateTime
            .now()
            .minusSeconds(
                ChronoUnit.SECONDS.between(
                    LocalDateTime.now(),
                    Tilstandsendringer.TilstandsendringEventDto.nestePåminnelsetidspunkt(
                        tilstand,
                        LocalDateTime.now(),
                        0
                    )
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
                                && inspektør.field(it, "vedtaksperiodeId").asText() == vedtaksperiodeId.toString()
                                && inspektør.field(it, "tilstand").asText() == tilstand
                    }
            }
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

        val påminnelse = hentPåminnelseFraDatabasen(dataSource, vedtaksperiodeId)

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

        val påminnelse = hentPåminnelseFraDatabasen(dataSource, vedtaksperiodeId)
        assertEquals(endringstidspunkt, påminnelse.endringstidspunkt)
        assertEquals(tilstand, påminnelse.tilstand)
    }

    private fun hentAntallPåminnelser(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
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
        return requireNotNull(using(sessionOf(dataSource)) { session ->
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
