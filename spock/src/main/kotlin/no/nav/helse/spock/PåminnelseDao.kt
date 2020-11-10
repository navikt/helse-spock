package no.nav.helse.spock

import kotliquery.*
import java.time.LocalDateTime
import javax.sql.DataSource

internal fun lagreTilstandsendring(
    dataSource: DataSource,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    vedtaksperiodeId: String,
    tilstand: String,
    endringstidspunkt: LocalDateTime,
    nestePåminnelsetidspunkt: LocalDateTime,
    originalJson: String
) {
    if (Tilstandsendringer.TilstandsendringEventDto.erSluttilstand(tilstand)) slettPåminnelse(dataSource, vedtaksperiodeId)
    else using(sessionOf(dataSource)) {
        it.run(
            queryOf(
                "INSERT INTO paminnelse (aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, endringstidspunkt, endringstidspunkt_nanos, neste_paminnelsetidspunkt, data) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, (to_json(?::json))) " +
                        "ON CONFLICT(vedtaksperiode_id) do " +
                        "UPDATE SET tilstand=EXCLUDED.tilstand, " +
                        "   endringstidspunkt=EXCLUDED.endringstidspunkt, " +
                        "   endringstidspunkt_nanos=EXCLUDED.endringstidspunkt_nanos, " +
                        "   neste_paminnelsetidspunkt=EXCLUDED.neste_paminnelsetidspunkt, " +
                        "   antall_ganger_paminnet=0, " +
                        "   data=EXCLUDED.data, " +
                        "   opprettet=now() " +
                        "WHERE (paminnelse.endringstidspunkt < EXCLUDED.endringstidspunkt) " +
                        "   OR (paminnelse.endringstidspunkt = EXCLUDED.endringstidspunkt AND paminnelse.endringstidspunkt_nanos < EXCLUDED.endringstidspunkt_nanos)",
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId,
                tilstand,
                endringstidspunkt,
                endringstidspunkt.nano,
                nestePåminnelsetidspunkt,
                originalJson
            ).asExecute
        )
    }
}

private fun slettPåminnelse(
    session: Session,
    vedtaksperiodeId: String
) = session.run(queryOf("DELETE FROM paminnelse WHERE vedtaksperiode_id = ?", vedtaksperiodeId).asExecute)

fun slettPåminnelse(dataSource: DataSource, vedtaksperiodeId: String) = using(sessionOf(dataSource)) { session ->
    slettPåminnelse(session, vedtaksperiodeId)
}

fun hentPåminnelser(dataSource: DataSource): List<PåminnelseDto> {
    return using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            tx.run(
                queryOf(
                    "SELECT id, aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, endringstidspunkt, antall_ganger_paminnet, neste_paminnelsetidspunkt " +
                            "FROM paminnelse " +
                            "WHERE neste_paminnelsetidspunkt <= now()"
                ).map {
                    PåminnelseDto(
                        id = it.string("id"),
                        aktørId = it.string("aktor_id"),
                        fødselsnummer = it.string("fnr"),
                        organisasjonsnummer = it.string("organisasjonsnummer"),
                        vedtaksperiodeId = it.string("vedtaksperiode_id"),
                        tilstand = it.string("tilstand"),
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
            "UPDATE paminnelse SET neste_paminnelsetidspunkt = ?, antall_ganger_paminnet = antall_ganger_paminnet + 1 WHERE id=?::BIGINT",
            påminnelse.nestePåminnelsetidspunkt,
            påminnelse.id
        ).asExecute
    )
}
