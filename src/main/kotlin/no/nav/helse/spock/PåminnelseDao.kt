package no.nav.helse.spock

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

internal fun lagrePerson(dataSource: DataSource, fødselsnummer: String, aktørId: String, tidsstempel: LocalDateTime) {
    @Language("PostgreSQL")
    val statement = """
        INSERT INTO person (fnr, aktor_id, siste_aktivitet) VALUES (:fnr, :aktor, :siste_aktivitet)
        ON CONFLICT (fnr) DO 
            UPDATE SET siste_aktivitet = excluded.siste_aktivitet
            WHERE person.siste_aktivitet < excluded.siste_aktivitet
    """
    sessionOf(dataSource).use {
        it.run(queryOf(statement, mapOf(
            "fnr" to fødselsnummer.toLong(),
            "aktor" to aktørId.toLong(),
            "siste_aktivitet" to tidsstempel
        )).asExecute)
    }
}

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
    lagrePerson(dataSource, fødselsnummer, aktørId, endringstidspunkt)
    if (Tilstandsendringer.TilstandsendringEventDto.erSluttilstand(tilstand)) slettPåminnelse(dataSource, vedtaksperiodeId)
    else sessionOf(dataSource).use {
        it.run(
            queryOf(
                "INSERT INTO paminnelse (aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, endringstidspunkt, endringstidspunkt_nanos, neste_paminnelsetidspunkt, data) " +
                        "VALUES (:aktorId, :fodselsnummer, :organisasjonsnummer, :vedtaksperiodeId, :tilstand, :endringstidspunkt, :endringstidspunktNano, :nestePaminnelsetidspunkt, (to_json(:originalJson::json))) " +
                        "ON CONFLICT(vedtaksperiode_id) do " +
                        "UPDATE SET tilstand=EXCLUDED.tilstand, " +
                        "   endringstidspunkt=EXCLUDED.endringstidspunkt, " +
                        "   endringstidspunkt_nanos=EXCLUDED.endringstidspunkt_nanos, " +
                        "   neste_paminnelsetidspunkt=EXCLUDED.neste_paminnelsetidspunkt, " +
                        "   antall_ganger_paminnet=0, " +
                        "   data=EXCLUDED.data, " +
                        "   opprettet=now() " +
                        "WHERE (paminnelse.tilstand != EXCLUDED.tilstand AND paminnelse.endringstidspunkt < EXCLUDED.endringstidspunkt) " +
                        "   OR (paminnelse.endringstidspunkt = EXCLUDED.endringstidspunkt AND paminnelse.endringstidspunkt_nanos < EXCLUDED.endringstidspunkt_nanos)",
                mapOf("aktorId" to aktørId,
                "fodselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "tilstand" to tilstand,
                "endringstidspunkt" to endringstidspunkt,
                "endringstidspunktNano" to endringstidspunkt.nano,
                "nestePaminnelsetidspunkt" to nestePåminnelsetidspunkt,
                "originalJson" to originalJson)
            ).asExecute
        )
    }
}

private fun slettPåminnelse(
    session: Session,
    vedtaksperiodeId: String
) = session.run(queryOf("DELETE FROM paminnelse WHERE vedtaksperiode_id = ?", vedtaksperiodeId).asExecute)

fun slettPåminnelse(dataSource: DataSource, vedtaksperiodeId: String) = sessionOf(dataSource).use { session ->
    slettPåminnelse(session, vedtaksperiodeId)
}

fun hentPåminnelser(dataSource: DataSource, block: (List<PåminnelseDto>) -> Unit) {
    @Language("PostgreSQL")
    val query = """
        SELECT id, aktor_id, fnr, organisasjonsnummer, vedtaksperiode_id, tilstand, endringstidspunkt, antall_ganger_paminnet, neste_paminnelsetidspunkt, skal_reberegnes 
        FROM paminnelse 
        WHERE neste_paminnelsetidspunkt <= now()
        LIMIT 20000
        FOR UPDATE SKIP LOCKED;
    """
    return sessionOf(dataSource).use { session ->
        session.transaction { tx ->
            tx.run(
                queryOf(query).map {
                    PåminnelseDto(
                        id = it.string("id"),
                        aktørId = it.string("aktor_id"),
                        fødselsnummer = it.string("fnr"),
                        organisasjonsnummer = it.string("organisasjonsnummer"),
                        vedtaksperiodeId = it.string("vedtaksperiode_id"),
                        tilstand = it.string("tilstand"),
                        endringstidspunkt = it.localDateTime("endringstidspunkt"),
                        antallGangerPåminnet = it.int("antall_ganger_paminnet") + 1,
                        ønskerReberegning = it.boolean("skal_reberegnes")
                    )
                }.asList
            )
                .takeUnless { it.isEmpty() }
                ?.also(block)
                ?.onEach {
                    oppdaterPåminnelse(tx, it)
                }
        }
    }
}

private fun oppdaterPåminnelse(transactionalSession: TransactionalSession, påminnelse: PåminnelseDto) {
    transactionalSession.run(
        queryOf(
            "UPDATE paminnelse SET skal_reberegnes=false, neste_paminnelsetidspunkt = :nestePaminnelsetidspunkt, antall_ganger_paminnet = antall_ganger_paminnet + 1 WHERE id=:id::BIGINT",
            mapOf("nestePaminnelsetidspunkt" to påminnelse.nestePåminnelsetidspunkt, "id" to påminnelse.id)

        ).asExecute
    )
}
