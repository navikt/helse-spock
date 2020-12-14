package no.nav.helse.spock

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.intellij.lang.annotations.Language
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

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        if (!påminnelseSchedule(lastReportTime)) return
        lagPåminnelser(context)
        lastReportTime = LocalDateTime.now()
    }

    private fun lagPåminnelser(context: RapidsConnection.MessageContext) {
        val påminnelser = hentPåminnelser()
        if (påminnelser.isEmpty()) return
        log.info("hentet ${påminnelser.size} personpåminnelser fra db")
        secureLogger.info("hentet ${påminnelser.size} personpåminnelser fra db")
        påminnelser.onEach { it.send(context) }
    }

    private fun hentPåminnelser(): List<Pair<String, String>> {
        @Language("PostgreSQL")
        val stmt = """
            SELECT fnr,aktor_id FROM person WHERE neste_paminnelsetidspunkt <= now() LIMIT 5000
        """
        val personer = mutableListOf<Long>()
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf(stmt).map { row ->
                val fnr = row.long("fnr")
                personer.add(fnr)
                Pair(fnr.toString().padStart(11, '0'), row.string("aktor_id"))
            }.asList).also {
                if (personer.isNotEmpty()) {
                    session.run(queryOf("""
                        UPDATE person SET neste_paminnelsetidspunkt = NULL
                        WHERE fnr IN(${personer.joinToString { "?" }})
                    """, *personer.toTypedArray()).asExecute)
                }
            }
        }
    }

    private fun Pair<String, String>.send(context: RapidsConnection.MessageContext) {
        val now = LocalDateTime.now()
        context.send(first, JsonMessage.newMessage(mapOf(
                "@id" to UUID.randomUUID(),
                "@event_name" to "person_påminnelse",
                "@opprettet" to "$now",
                "fødselsnummer" to first,
                "aktørId" to second
        )).toJson())
    }
}
