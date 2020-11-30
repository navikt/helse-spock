package no.nav.helse.spock

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class UtbetalingPåminnelser(
        rapidsConnection: RapidsConnection,
        private val dataSource: DataSource,
        schedule: Duration
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(UtbetalingPåminnelser::class.java)
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }

    private var lastReportTime = LocalDateTime.MIN
    private val påminnelseSchedule = { lastReportTime: LocalDateTime ->
        lastReportTime < LocalDateTime.now().minusSeconds(schedule.toSeconds())
    }

    init {
        River(rapidsConnection).register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        if (!påminnelseSchedule(lastReportTime)) return
        lagPåminnelser(context)
        lastReportTime = LocalDateTime.now()
    }

    private fun lagPåminnelser(context: RapidsConnection.MessageContext) {
        val påminnelser = hentPåminnelser()
        if (påminnelser.isEmpty()) return
        log.info("hentet ${påminnelser.size} utbetalingpåminnelser fra db")
        secureLogger.info("hentet ${påminnelser.size} utbetalingpåminnelser fra db")
        påminnelser.onEach { it.send(context, dataSource) }
    }

    private fun hentPåminnelser(): List<Utbetalingpåminnelse> {
        @Language("PostgreSQL")
        val stmt = """
            SELECT * FROM utbetaling 
            WHERE neste_paminnelsetidspunkt <= now()
        """
        return using(sessionOf(dataSource)) {
            it.run(queryOf(stmt).map { row ->
                Utbetalingpåminnelse(
                        aktørId = row.string("aktor_id"),
                        fødselsnummer = row.string("fnr"),
                        organisasjonsnummer = row.string("orgnr"),
                        utbetalingId = UUID.fromString(row.string("id")),
                        type = row.string("type"),
                        status = row.string("status"),
                        endringstidspunkt = row.localDateTime("endringstidspunkt"),
                        endringstidspunktNanos = row.int("endringstidspunkt_nanos"),
                        data = row.string("data"),
                        antallGangerPåminnet = row.int("antall_ganger_paminnet")
                )
            }.asList)
        }
    }

    internal class Utbetalingpåminnelse(
            private val aktørId: String,
            private val fødselsnummer: String,
            private val organisasjonsnummer: String,
            private val utbetalingId: UUID,
            private val type: String,
            private val status: String,
            private val endringstidspunkt: LocalDateTime,
            private val endringstidspunktNanos: Int = endringstidspunkt.nano,
            private val data: String,
            private val antallGangerPåminnet: Int = 0
    ) {
        private fun lagre(dataSource: DataSource) {
            @Language("PostgreSQL")
            val statement = """
            INSERT INTO utbetaling (id, aktor_id, fnr, orgnr, type, 
                status, endringstidspunkt, endringstidspunkt_nanos, neste_paminnelsetidspunkt, data)
            VALUES (:id, :aktorId, :fnr, :orgnr, CAST(:type as utbetaling_type), :status, :endringstidspunkt,
                :endringstidspunktNanos, :nestePaminnelsetidspunkt, to_json(:data::json))
            ON CONFLICT (id) DO UPDATE SET 
                status=EXCLUDED.status,
                endringstidspunkt=EXCLUDED.endringstidspunkt,
                endringstidspunkt_nanos=EXCLUDED.endringstidspunkt_nanos,
                neste_paminnelsetidspunkt=EXCLUDED.neste_paminnelsetidspunkt,
                antall_ganger_paminnet=0,
                data=EXCLUDED.data,
                opprettet=now()
            WHERE (utbetaling.endringstidspunkt < EXCLUDED.endringstidspunkt) 
                OR (utbetaling.endringstidspunkt = EXCLUDED.endringstidspunkt AND utbetaling.endringstidspunkt_nanos < EXCLUDED.endringstidspunkt_nanos)
        """
            using(sessionOf(dataSource)) {
                it.run(queryOf(statement, mapOf(
                        "id" to utbetalingId,
                        "aktorId" to aktørId,
                        "fnr" to fødselsnummer,
                        "orgnr" to organisasjonsnummer,
                        "type" to type,
                        "status" to status,
                        "endringstidspunkt" to endringstidspunkt.withNano(0),
                        "endringstidspunktNanos" to endringstidspunktNanos,
                        "nestePaminnelsetidspunkt" to nestePåminnelsetidspunkt(endringstidspunkt, status),
                        "data" to data
                )).asExecute)
            }
        }

        private fun oppdater(dataSource: DataSource, tidspunkt: LocalDateTime) {
            @Language("PostgreSQL")
            val statement = """
                UPDATE utbetaling SET 
                    antall_ganger_paminnet = antall_ganger_paminnet + 1,
                    neste_paminnelsetidspunkt = :nestePaminnelsetidspunkt
                WHERE id = :id
            """
            using(sessionOf(dataSource)) {
                it.run(queryOf(statement, mapOf(
                        "id" to utbetalingId,
                        "nestePaminnelsetidspunkt" to nestePåminnelsetidspunkt(tidspunkt, status)
                )).asExecute)
            }
        }

        internal fun send(context: RapidsConnection.MessageContext, dataSource: DataSource) {
            val now = LocalDateTime.now()
            oppdater(dataSource, now)
            context.send(fødselsnummer, JsonMessage.newMessage(mapOf(
                    "@id" to UUID.randomUUID(),
                    "@event_name" to "utbetalingpåminnelse",
                    "@opprettet" to "$now",
                    "aktørId" to aktørId,
                    "fødselsnummer" to fødselsnummer,
                    "organisasjonsnummer" to organisasjonsnummer,
                    "utbetalingId" to utbetalingId,
                    "status" to status,
                    "endringstidspunkt" to "$endringstidspunkt",
                    "antallGangerPåminnet" to antallGangerPåminnet + 1
            )).toJson())
        }

        internal companion object {
            internal fun nestePåminnelsetidspunkt(tidspunkt: LocalDateTime, status: String): LocalDateTime? {
                return when (status) {
                    in listOf("GODKJENT", "SENDT", "OVERFØRT") -> tidspunkt.plusHours(1)
                    else -> null
                }
            }

            internal fun opprett(packet: JsonMessage, dataSource: DataSource): Utbetalingpåminnelse {
                val endringstidspunkt = packet["@opprettet"].asLocalDateTime()
                val påminnelse = Utbetalingpåminnelse(
                        aktørId = packet["aktørId"].asText(),
                        fødselsnummer = packet["fødselsnummer"].asText(),
                        organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                        utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
                        type = packet["type"].asText(),
                        status = packet["gjeldendeStatus"].asText(),
                        endringstidspunkt = endringstidspunkt,
                        data = packet.toJson()
                )
                påminnelse.lagre(dataSource)
                return påminnelse
            }
        }
    }
}
