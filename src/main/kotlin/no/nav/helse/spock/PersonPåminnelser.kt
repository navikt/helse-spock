package no.nav.helse.spock

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PersonPåminnelser(
        rapidsConnection: RapidsConnection,
        private val dataSource: DataSource,
        schedule: Duration
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(PersonPåminnelser::class.java)
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }

    private var lastReportTime = LocalDateTime.MIN
    private val påminnelseSchedule = { lastReportTime: LocalDateTime ->
        lastReportTime < LocalDateTime.now().minusSeconds(schedule.toSeconds())
    }

    init {
        River(rapidsConnection).register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (!påminnelseSchedule(lastReportTime)) return
        lagPåminnelser(context)
        lastReportTime = LocalDateTime.now()
    }

    private fun lagPåminnelser(context: MessageContext) {
        hentPåminnelser(dataSource) { påminnelser ->
            log.info("hentet ${påminnelser.size} personpåminnelser fra db")
            secureLogger.info("hentet ${påminnelser.size} personpåminnelser fra db")
            påminnelser.onEach { it.send(context) }
        }
    }

    private fun hentPåminnelser(dataSource: DataSource, block: (List<Pair<Long, String>>) -> Unit) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "SELECT fnr, aktor_id FROM person WHERE neste_paminnelsetidspunkt <= now() LIMIT 20000 FOR UPDATE SKIP LOCKED;"
                    ).map {
                        it.long("fnr") to it.string("aktor_id")
                    }.asList
                )
                    .takeUnless { it.isEmpty() }
                    ?.also(block)
                    ?.also { personer ->
                        val fødselsnummer = personer.map { it.first }
                        tx.run(queryOf("""
                            UPDATE person SET neste_paminnelsetidspunkt = NULL
                            WHERE fnr IN(${fødselsnummer.joinToString { "?" }})
                        """, *fødselsnummer.toTypedArray()).asExecute)
                    }
            }
        }
    }

    private fun Pair<Long, String>.send(context: MessageContext) {
        val now = LocalDateTime.now()
        val fnr = first.toString().padStart(11, '0')
        context.publish(fnr, JsonMessage.newMessage(mapOf(
                "@id" to UUID.randomUUID(),
                "@event_name" to "person_påminnelse",
                "@opprettet" to "$now",
                "fødselsnummer" to fnr,
                "aktørId" to second
        )).toJson())
    }
}
