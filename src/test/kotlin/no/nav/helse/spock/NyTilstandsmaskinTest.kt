package no.nav.helse.spock

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NyTilstandsmaskinTest {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:13")
    private lateinit var dataSource: DataSource
    private lateinit var flyway: FluentConfiguration

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

        dataSource = dsbuilder.getDataSource()
        flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
    }

    @AfterEach
    fun `clear database`() {
        flyway.load().clean()
    }

    @AfterAll
    fun `stop postgres`() {
        postgres.stop()
    }


    private val inntektsmeldingTilstander = setOf(
        "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
        "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
        "AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE",
        "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE"
    )

    @Test
    fun `Migrere tilstander som venter på inntektsmelding til AvventerInntekstmeldingEllerHistorikk`() {
        flyway.target(MigrationVersion.fromVersion("38")).load().migrate()
        inntektsmeldingTilstander.forEach {
            lagreTilstandsendring(
                dataSource,
                "1000000000",
                "111111111",
                "ORGNUMMER",
                UUID.randomUUID().toString(),
                it,
                LocalDateTime.now(),
                LocalDateTime.now(),
                """{ "originalJson": "json" }"""
            )
        }
        assertEquals(inntektsmeldingTilstander, hentPåminnelser(dataSource).toSet())
        flyway.target(MigrationVersion.fromVersion("39.1")).load().migrate()
        assertEquals(
            listOf(
                "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
                "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
                "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
                "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
            ),
            hentPåminnelser(dataSource))
    }

    private val avventerBlokkerendeTilstander = setOf(
        "AVVENTER_UFERDIG",
        "AVVENTER_ARBEIDSGIVERE"
    )

    @Test
    fun `Migrere tilstander som venter på uferdig periode eller andre arbeidsgivere til AvventerBlokkerendePeriode`() {
        flyway.target(MigrationVersion.fromVersion("39.1")).load().migrate()
        avventerBlokkerendeTilstander.forEach {
            lagreTilstandsendring(
                dataSource,
                "1000000000",
                "111111111",
                "ORGNUMMER",
                UUID.randomUUID().toString(),
                it,
                LocalDateTime.now(),
                LocalDateTime.now(),
                """{ "originalJson": "json" }"""
            )
        }
        assertEquals(avventerBlokkerendeTilstander, hentPåminnelser(dataSource).toSet())
        flyway.target(MigrationVersion.fromVersion("39.2")).load().migrate()
        assertEquals(
            listOf("AVVENTER_BLOKKERENDE_PERIODE", "AVVENTER_BLOKKERENDE_PERIODE"),
            hentPåminnelser(dataSource)
        )
    }

    private val tilstanderUtenMottatSøknad = setOf(
        "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE",
        "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
        "AVVENTER_SØKNAD_FERDIG_GAP",
        "AVVENTER_SØKNAD_UFERDIG_GAP",
        "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
        "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
        "MOTTATT_SYKMELDING_FERDIG_GAP",
        "MOTTATT_SYKMELDING_UFERDIG_GAP"
    )

    @Test
    fun `Slette påminnelser med tilstander som ikke har mottatt søknad`() {
        flyway.target(MigrationVersion.fromVersion("39.2")).load().migrate()
        tilstanderUtenMottatSøknad.forEach {
            lagreTilstandsendring(
                dataSource,
                "1000000000",
                "111111111",
                "ORGNUMMER",
                UUID.randomUUID().toString(),
                it,
                LocalDateTime.now(),
                LocalDateTime.now(),
                """{ "originalJson": "json" }"""
            )
        }
        assertEquals(tilstanderUtenMottatSøknad, hentPåminnelser(dataSource).toSet())
        flyway.target(MigrationVersion.fromVersion("39.3")).load().migrate()
        assertEquals(emptyList<String>(), hentPåminnelser(dataSource))
    }

    private fun hentPåminnelser(dataSource: DataSource): List<String> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                    queryOf("SELECT tilstand FROM paminnelse").map { it.string("tilstand") }.asList
                )
        }
    }

}