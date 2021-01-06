package no.nav.helse.spock

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpesialistPåminnelserTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var rapid: TestRapid
    private lateinit var dataSource: DataSource

    val OPPGAVE_ID: Long = 1
    val FNR = "01019000000"


    @BeforeAll
    fun `start postgres`() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        val dsbuilder = DataSourceBuilder(
            mapOf(
                "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
            )
        )

        dsbuilder.migrate()
        dataSource = dsbuilder.getDataSource()
        val oppgaveMakstidPåminnelseDao = OppgaveMakstidPåminnelseDao(dataSource)

        rapid = TestRapid().apply {
            OppgavePåminnelser(this, oppgaveMakstidPåminnelseDao)
            Påminnelser(this, dataSource, oppgaveMakstidPåminnelseDao, Duration.ofMillis(1))
        }
    }

    @AfterAll
    fun `stop postgres`() {
        embeddedPostgres.close()
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
    }

    @Test
    fun `Oppretter oppgave_makstid_påminnelse etter et oppgave_opprettet event`() {
        rapid.sendTestMessage(oppgaveEvent("oppgave_opprettet", OPPGAVE_ID, FNR))
        val oppgavePåminnelse = hentOppgaveMakstidFraDatabasen(dataSource, OPPGAVE_ID)
        val nestePåminnelsetidspunkt = oppgavePåminnelse["nestePåminnelsetidspunkt"] as LocalDateTime
        assertEquals(LocalDate.now().plusDays(4).dayOfMonth, nestePåminnelsetidspunkt.dayOfMonth)
    }

    @Test
    fun `Oppdaterer oppgave_makstid_påminnelse som maser om 14 dager etter et oppgave_oppdatert event med makstid 14 dager`() {
        rapid.sendTestMessage(oppgaveEvent("oppgave_opprettet", OPPGAVE_ID, FNR))
        rapid.sendTestMessage(
            oppgaveEvent(
                "oppgave_oppdatert",
                OPPGAVE_ID,
                FNR,
                makstid = LocalDateTime.now().plusDays(14)
            )
        )
        val oppgavePåminnelse = hentOppgaveMakstidFraDatabasen(dataSource, OPPGAVE_ID)
        val nestePåminnelsetidspunkt = oppgavePåminnelse["nestePåminnelsetidspunkt"] as LocalDateTime
        assertEquals(LocalDate.now().plusDays(14).dayOfMonth, nestePåminnelsetidspunkt.dayOfMonth)
    }

    @Test
    fun `Sletter oppgave_makstid_påminnelse når oppgave_oppdatert event har status Ferdigstilt`() {
        rapid.sendTestMessage(oppgaveEvent("oppgave_opprettet", OPPGAVE_ID, FNR))
        rapid.sendTestMessage(oppgaveEvent("oppgave_oppdatert", OPPGAVE_ID, FNR, status = "Ferdigstilt"))
        assertThrows<IllegalArgumentException> { hentOppgaveMakstidFraDatabasen(dataSource, OPPGAVE_ID) }
    }

    @Test
    fun `Sletter oppgave_makstid_påminnelse når oppgave_oppdatert event har status MakstidOppnådd`() {
        rapid.sendTestMessage(oppgaveEvent("oppgave_opprettet", OPPGAVE_ID, FNR))
        rapid.sendTestMessage(oppgaveEvent("oppgave_oppdatert", OPPGAVE_ID, FNR, status = "MakstidOppnådd"))
        assertThrows<IllegalArgumentException> { hentOppgaveMakstidFraDatabasen(dataSource, OPPGAVE_ID) }
    }

    @Test
    fun `Sletter oppgave_makstid_påminnelse når oppgave_oppdatert event har status Invalidert`() {
        rapid.sendTestMessage(oppgaveEvent("oppgave_opprettet", OPPGAVE_ID, FNR))
        rapid.sendTestMessage(oppgaveEvent("oppgave_oppdatert", OPPGAVE_ID, FNR, status = "Invalidert"))
        assertThrows<IllegalArgumentException> { hentOppgaveMakstidFraDatabasen(dataSource, OPPGAVE_ID) }
    }

    @Test
    fun `lager oppgave_makstid_påminnelse når makstid har vært`() {
        rapid.sendTestMessage(
            oppgaveEvent(
                "oppgave_opprettet",
                OPPGAVE_ID,
                FNR,
                makstid = LocalDateTime.now().minusDays(1)
            )
        )

        await("skal produsere påminnelse")
            .atMost(10, TimeUnit.SECONDS)
            .until {
                rapid.sendTestMessage("{}") // create noise on the rapid

                val inspektør = rapid.inspektør
                if (inspektør.size == 0) false
                else (0 until inspektør.size)
                    .any {
                        inspektør.field(it, "@event_name").asText() == "påminnelse_oppgave_makstid"
                                && inspektør.field(it, "oppgaveId").asLong() == OPPGAVE_ID
                                && inspektør.field(it, "fødselsnummer").asText() == FNR
                    }
            }

        val antallGangerPåminnet = hentOppgaveMakstidFraDatabasen(dataSource, OPPGAVE_ID)["antallGangerPåminnet"]
        val nestePåminnelsetidspunkt = hentOppgaveMakstidFraDatabasen(dataSource, OPPGAVE_ID)["nestePåminnelsetidspunkt"] as LocalDateTime

        assertEquals(1, antallGangerPåminnet)
        assertEquals(LocalDateTime.now().plusHours(6).hour, nestePåminnelsetidspunkt.hour)
    }

    private fun oppgaveEvent(
        eventName: String,
        oppgaveId: Long,
        fodselsnummer: String,
        status: String = "AvventerSaksbehandler",
        makstid: LocalDateTime = LocalDateTime.now().plusDays(4)
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to eventName,
            "@id" to "030001BD-8FBA-4324-9725-D618CE5B83E9",
            "status" to status,
            "oppgaveId" to oppgaveId,
            "fødselsnummer" to fodselsnummer,
            "makstid" to makstid
        )
    ).toJson()


    private fun hentOppgaveMakstidFraDatabasen(dataSource: DataSource, oppgaveId: Long): Map<String, Any> {
        return requireNotNull(using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "SELECT * FROM oppgave_makstid_paminnelse WHERE oppgave_id = ?", oppgaveId
                    ).map {
                        mapOf(
                            "id" to it.string("id"),
                            "fødselsnummer" to it.string("fodselsnummer"),
                            "nestePåminnelsetidspunkt" to it.localDateTime("neste_paminnelsetidspunkt"),
                            "antallGangerPåminnet" to it.int("antall_ganger_paminnet")
                        )
                    }.asSingle
                )
            }
        }) { "Fant ikke oppgave for oppgaveId=$oppgaveId" }
    }
}
