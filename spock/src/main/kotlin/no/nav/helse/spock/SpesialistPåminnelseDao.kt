package no.nav.helse.spock

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.time.LocalDateTime
import javax.sql.DataSource

class SpesialistPåminnelseDao(private val dataSource: DataSource) {

    fun finnEllerOpprettPåminnelse(
        referanse: String,
        fødselsnummer: String,
        endringstidspunkt: LocalDateTime,
        timeout: Long
    ) {
        using(sessionOf(dataSource)) { session ->
            val påminnelse = finnPåminnelse(referanse)
            val nestePåminnelse = endringstidspunkt.plusSeconds(timeout)
            if (påminnelse == null) {
                opprettPåminnelse(referanse, fødselsnummer, endringstidspunkt, timeout, nestePåminnelse)
            } else {
                oppdaterPåminnelse(påminnelse.id, endringstidspunkt, timeout, nestePåminnelse)
            }
        }
    }

    private fun finnPåminnelse(referanse: String) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT * FROM spesialist_paminnelse WHERE referanse=?;", referanse)
                .map(::toDTO).asSingle
        )
    }

    private fun opprettPåminnelse(
        referanse: String,
        fødselsnummer: String,
        endringstidspunkt: LocalDateTime,
        timeout: Long,
        nestePåminnelse: LocalDateTime
    ) = using(sessionOf(dataSource)) { session ->
        queryOf(
            "INSERT INTO spesialist_paminnelse(fodselsnummer, referanse, endringstidspunkt, timeout, neste_paminnelsetidspunkt) VALUES(?, ?, ?, ?, ?);",
            fødselsnummer, referanse, endringstidspunkt, timeout, nestePåminnelse
        ).asUpdate
    }

    private fun oppdaterPåminnelse(
        påminnelseId: Long,
        endringstidspunkt: LocalDateTime,
        timeout: Long,
        nestePåminnelse: LocalDateTime
    ) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE spesialist_paminnelse SET endringstidspunkt=?, timeout=?, neste_paminnelsetidspunkt=? WHERE id=?;",
                endringstidspunkt, timeout, nestePåminnelse, påminnelseId
            ).asUpdate
        )
    }

    fun hentPåminnelser(): List<SpesialistPåminnelseDto> = using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            tx.run(
                queryOf("SELECT * FROM spesialist_paminnelse WHERE now() > neste_paminnelsetidspunkt;")
                    .map(::toDTO).asList
            ).also { påminnelser ->
                påminnelser.forEach {
                    oppdaterTidspunkt(tx, it)
                }
            }
        }
    }

    private fun toDTO(it: Row): SpesialistPåminnelseDto {
        return SpesialistPåminnelseDto(
            it.long("id"),
            it.string("referanse"),
            it.string("fodselsnummer"),
            it.int("antall_ganger_paminnet"),
            it.localDateTime("endringstidspunkt"),
            it.long("timeout")
        )
    }

    private fun oppdaterTidspunkt(tx: TransactionalSession, dto: SpesialistPåminnelseDto) {
        tx.run(
            queryOf(
                "UPDATE spesialist_paminnelse SET neste_paminnelsetidspunkt = (now() + timeout * interval '1 second'), antall_ganger_paminnet = antall_ganger_paminnet + 1 WHERE id=?::BIGINT;",
                dto.id
            ).asUpdate
        )
    }
}