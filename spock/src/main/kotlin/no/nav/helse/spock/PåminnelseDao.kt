package no.nav.helse.spock

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

fun lagreTilstandsendring(dataSource: DataSource, event: Tilstandsendringer.TilstandsendringEventDto) {
    using(sessionOf(dataSource)) {
        it.run(
            queryOf(
                "INSERT INTO paminnelse (aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, timeout, endringstidspunkt, endringstidspunkt_nanos, neste_paminnelsetidspunkt, data) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, (to_json(?::json))) " +
                        "ON CONFLICT(vedtaksperiode_id) do " +
                        "UPDATE SET tilstand=EXCLUDED.tilstand, " +
                        "   timeout=EXCLUDED.timeout, " +
                        "   endringstidspunkt=EXCLUDED.endringstidspunkt, " +
                        "   endringstidspunkt_nanos=EXCLUDED.endringstidspunkt_nanos, " +
                        "   neste_paminnelsetidspunkt=EXCLUDED.neste_paminnelsetidspunkt, " +
                        "   antall_ganger_paminnet=0, " +
                        "   data=EXCLUDED.data, " +
                        "   opprettet=now() " +
                        "WHERE (paminnelse.endringstidspunkt < EXCLUDED.endringstidspunkt) " +
                        "   OR (paminnelse.endringstidspunkt = EXCLUDED.endringstidspunkt AND paminnelse.endringstidspunkt_nanos < EXCLUDED.endringstidspunkt_nanos)",
                event.aktørId,
                event.fødselsnummer,
                event.organisasjonsnummer,
                event.vedtaksperiodeId,
                event.tilstand,
                event.timeout,
                event.endringstidspunkt,
                event.endringstidspunkt.nano,
                event.endringstidspunkt.plusSeconds(event.timeout),
                event.originalJson
            ).asExecute
        )
    }
}

fun hentPåminnelser(dataSource: DataSource): List<PåminnelseDto> {
    return using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            tx.run(queryOf("DELETE FROM paminnelse WHERE timeout=0").asExecute)
            tx.run(
                queryOf(
                    "SELECT id, aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, timeout, endringstidspunkt, antall_ganger_paminnet, neste_paminnelsetidspunkt " +
                            "FROM paminnelse " +
                            "WHERE timeout > 0 AND neste_paminnelsetidspunkt <= now()"
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
            ).onEach {
                oppdaterPåminnelse(tx, it)
            }
        }
    }
}

private fun oppdaterPåminnelse(transactionalSession: TransactionalSession, påminnelse: PåminnelseDto) {
    transactionalSession.run(
        queryOf(
            "UPDATE paminnelse SET neste_paminnelsetidspunkt = (now() + timeout * interval '1 second'), antall_ganger_paminnet = antall_ganger_paminnet + 1 WHERE id=?::BIGINT",
            påminnelse.id
        ).asExecute
    )
}
