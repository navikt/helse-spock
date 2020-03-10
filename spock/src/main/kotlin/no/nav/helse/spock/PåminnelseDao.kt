package no.nav.helse.spock

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

fun lagreTilstandsendring(dataSource: DataSource, event: Påminnelser.TilstandsendringEventDto) {
    using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            tx.run(
                queryOf(
                    "INSERT INTO paminnelse (aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, timeout, endringstidspunkt, neste_paminnelsetidspunkt, data) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, (to_json(?::json)))",
                    event.aktørId,
                    event.fødselsnummer,
                    event.organisasjonsnummer,
                    event.vedtaksperiodeId,
                    event.tilstand,
                    event.timeout,
                    event.endringstidspunkt,
                    event.endringstidspunkt.plusSeconds(event.timeout),
                    event.originalJson
                ).asExecute
            )

            hentGjeldendeTilstand(tx, event.vedtaksperiodeId)?.also { nyesteEndringsevent ->
                if (nyesteEndringsevent.timeout <= 0) {
                    tx.run(
                        queryOf("DELETE FROM paminnelse WHERE vedtaksperiode_id=?",
                            event.vedtaksperiodeId).asExecute)
                } else {
                    tx.run(
                        queryOf("DELETE FROM paminnelse WHERE vedtaksperiode_id=? AND id != ?::bigint",
                            event.vedtaksperiodeId, nyesteEndringsevent.id).asExecute)
                }
            }
        }
    }
}

class GjeldeneTilstand(val id: String, val timeout: Long)
private fun hentGjeldendeTilstand(session: Session, vedtaksperiodeId: String): GjeldeneTilstand? {
    return session.run(
        queryOf("SELECT id, timeout FROM paminnelse " +
                "WHERE vedtaksperiode_id = ? " +
                "ORDER BY endringstidspunkt DESC, opprettet DESC " +
                "LIMIT 1", vedtaksperiodeId).map {
            GjeldeneTilstand(
                id = it.string(1),
                timeout = it.long(2)
            )
        }.asSingle
    )
}

fun hentPåminnelser(dataSource: DataSource): List<PåminnelseDto> {
    return using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT id, aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, timeout, endringstidspunkt, antall_ganger_paminnet, neste_paminnelsetidspunkt " +
                        "FROM paminnelse " +
                        "WHERE timeout > 0 AND neste_paminnelsetidspunkt <= now() " +
                        "AND id IN (SELECT DISTINCT ON (vedtaksperiode_id) id FROM paminnelse ORDER BY vedtaksperiode_id, endringstidspunkt DESC, opprettet DESC)"
            ).map {
                PåminnelseDto(
                    id = it.string("id"),
                    aktørId = it.string("aktor_id"),
                    fødselsnummer = it.string("fnr"),
                    organisasjonsnummer = it.string("organisasjonsnummer"),
                    vedtaksperiodeId = it.string("vedtaksperiode_id"),
                    tilstand = it.string("tilstand"),
                    timeout = it.long("timeout"),
                    endringstidspunkt = it.localDateTime("endringstidspunkt"),
                    antallGangerPåminnet = it.int("antall_ganger_paminnet") + 1
                )
            }.asList
        )
    }
}

fun oppdaterPåminnelse(dataSource: DataSource, påminnelse: PåminnelseDto) {
    using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE paminnelse SET neste_paminnelsetidspunkt = (now() + timeout * interval '1 second'), antall_ganger_paminnet = antall_ganger_paminnet + 1 WHERE id=?::BIGINT",
                påminnelse.id
            ).asExecute
        )
    }
}
