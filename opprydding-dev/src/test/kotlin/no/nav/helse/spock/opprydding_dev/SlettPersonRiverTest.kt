package no.nav.helse.spock.opprydding_dev

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.test_support.TestDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SlettPersonRiverTest {
    private lateinit var dataSource: TestDataSource
    private lateinit var rapid: TestRapid

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        rapid =
            TestRapid().apply {
                SlettPersonRiver(this, dataSource.ds)
            }
    }

    @AfterEach
    fun teardown() {
        databaseContainer.droppTilkobling(dataSource)
    }

    @Test
    fun `sletter all data for en person`() {
        val fødselsnummer = "01019012345"
        insertPerson(fødselsnummer)
        insertPåminnelse(fødselsnummer)
        insertUtbetaling(fødselsnummer)

        assertEquals(1, antallRader("person", fødselsnummer))
        assertEquals(1, antallRader("paminnelse", fødselsnummer))
        assertEquals(1, antallRader("utbetaling", fødselsnummer))

        rapid.sendTestMessage(slettPersonMelding(fødselsnummer))

        assertEquals(0, antallRader("person", fødselsnummer))
        assertEquals(0, antallRader("paminnelse", fødselsnummer))
        assertEquals(0, antallRader("utbetaling", fødselsnummer))
    }

    @Test
    fun `sletter ikke data for andre personer`() {
        val fødselsnummer1 = "01019012345"
        val fødselsnummer2 = "02029012345"
        insertPerson(fødselsnummer1)
        insertPerson(fødselsnummer2)

        rapid.sendTestMessage(slettPersonMelding(fødselsnummer1))

        assertEquals(0, antallRader("person", fødselsnummer1))
        assertEquals(1, antallRader("person", fødselsnummer2))
    }

    @Test
    fun `publiserer person_slettet etter sletting`() {
        val fødselsnummer = "01019012345"
        insertPerson(fødselsnummer)

        rapid.sendTestMessage(slettPersonMelding(fødselsnummer))

        assertEquals(1, rapid.inspektør.size)
        val melding = rapid.inspektør.message(0)
        assertEquals("person_slettet", melding.path("@event_name").asString())
        assertEquals(fødselsnummer, melding.path("fødselsnummer").asString())
    }

    @Test
    fun `gjør ingenting for person uten data`() {
        rapid.sendTestMessage(slettPersonMelding("99999999999"))

        assertEquals(1, rapid.inspektør.size)
    }

    private fun antallRader(
        tabell: String,
        fødselsnummer: String,
    ): Int {
        val fnrParam: Any = if (tabell == "person") fødselsnummer.toLong() else fødselsnummer
        return sessionOf(dataSource.ds).use { session ->
            session.run(
                queryOf("SELECT COUNT(*) AS antall FROM $tabell WHERE fnr = :fnr", mapOf("fnr" to fnrParam))
                    .map { it.int("antall") }
                    .asSingle,
            )!!
        }
    }

    private fun insertPerson(fødselsnummer: String) {
        sessionOf(dataSource.ds).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO person (fnr, siste_aktivitet) VALUES (:fnr, :siste_aktivitet)",
                    mapOf(
                        "fnr" to fødselsnummer.toLong(),
                        "siste_aktivitet" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }

    private fun insertPåminnelse(fødselsnummer: String) {
        sessionOf(dataSource.ds).use { session ->
            session.run(
                queryOf(
                    """
                    INSERT INTO paminnelse
                        (fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, endringstidspunkt, endringstidspunkt_nanos, neste_paminnelsetidspunkt, data)
                    VALUES
                        (:fnr, '987654321', :vedtaksperiode_id, 'AVVENTER_GODKJENNING', :now, 0, :now, '{}')
                    """,
                    mapOf(
                        "fnr" to fødselsnummer,
                        "vedtaksperiode_id" to
                            java.util.UUID
                                .randomUUID()
                                .toString(),
                        "now" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }

    private fun insertUtbetaling(fødselsnummer: String) {
        sessionOf(dataSource.ds).use { session ->
            session.run(
                queryOf(
                    """
                    INSERT INTO utbetaling
                        (id, fnr, orgnr, type, status, endringstidspunkt, endringstidspunkt_nanos, data)
                    VALUES
                        (:id, :fnr, '987654321', 'UTBETALING', 'UTBETALT', :now, 0, '{}')
                    """,
                    mapOf(
                        "id" to java.util.UUID.randomUUID(),
                        "fnr" to fødselsnummer,
                        "now" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }

    private fun slettPersonMelding(fødselsnummer: String) =
        """
        {
          "@event_name": "slett_person",
          "@id": "${java.util.UUID.randomUUID()}",
          "fødselsnummer": "$fødselsnummer"
        }
        """.trimIndent()
}
