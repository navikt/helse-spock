package no.nav.helse.spock

import kotliquery.*
import java.time.LocalDateTime
import javax.sql.DataSource

class OppgaveMakstidPåminnelseDao(private val dataSource: DataSource) {

    fun behandlePåminnelse(
            oppgaveId: Long,
            fødselsnummer: String,
            makstid: LocalDateTime,
            status: String,
            eventId: String,
    ) {
            val påminnelse = finnPåminnelse(oppgaveId)

            if (påminnelse == null) {
                opprettPåminnelse(oppgaveId, fødselsnummer, makstid, eventId)
            } else if (status == "Ferdigstilt" || status == "MakstidOppnådd" || status == "Invalidert") {
                slettPåminnelse(oppgaveId)
            } else {
                oppdaterPåminnelse(oppgaveId, makstid, eventId)
            }
    }

    private fun finnPåminnelse(oppgaveId: Long) = using(sessionOf(dataSource)) { session ->
        session.run(
                queryOf("SELECT * FROM oppgave_makstid_paminnelse WHERE oppgave_id=?;", oppgaveId)
                        .map{ it.long("id") }.asSingle
        )
    }

    private fun opprettPåminnelse(
            oppgaveId: Long,
            fødselsnummer: String,
            nestePåminnelse: LocalDateTime,
            eventId: String
    ) = using(sessionOf(dataSource)) { session ->
        session.run(queryOf(
                "INSERT INTO oppgave_makstid_paminnelse(oppgave_id, fodselsnummer, neste_paminnelsetidspunkt, event_id) VALUES(?, ?, ?, ?);",
                oppgaveId, fødselsnummer, nestePåminnelse, eventId
        ).asUpdate)
    }

    private fun slettPåminnelse(oppgaveId: Long) = using(sessionOf(dataSource)) {
        session -> session.run(queryOf("DELETE FROM oppgave_makstid_paminnelse WHERE oppgave_id = ?", oppgaveId).asExecute)
    }

    private fun oppdaterPåminnelse(
            oppgaveId: Long,
            nestePåminnelse: LocalDateTime,
            eventId: String
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
                queryOf(
                        "UPDATE oppgave_makstid_paminnelse SET neste_paminnelsetidspunkt=?, event_id=? WHERE oppgave_id=?;",
                        nestePåminnelse, eventId, oppgaveId
                ).asUpdate
        )
    }

    fun hentPåminnelser(): List<OppgaveMakstidPåminnelseDto> = using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            tx.run(
                    queryOf("SELECT * FROM oppgave_makstid_paminnelse WHERE now() > neste_paminnelsetidspunkt;")
                            .map(::toDTO).asList
            ).also { påminnelser ->
                påminnelser.forEach {
                    oppdaterTidspunkt(tx, it)
                }
            }
        }
    }

    private fun toDTO(it: Row): OppgaveMakstidPåminnelseDto {
        return OppgaveMakstidPåminnelseDto(
                it.long("oppgave_id"),
                it.string("fodselsnummer")
        )
    }

    private fun oppdaterTidspunkt(tx: TransactionalSession, dto: OppgaveMakstidPåminnelseDto) {
        tx.run(
                queryOf(
                        "UPDATE oppgave_makstid_paminnelse SET neste_paminnelsetidspunkt = (now() + INTERVAL '6 HOURS'), antall_ganger_paminnet = antall_ganger_paminnet + 1 WHERE oppgave_id=?;",
                        dto.oppgaveId
                ).asUpdate
        )
    }
}
