package no.nav.helse.spock

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotliquery.queryOf
import kotliquery.sessionOf
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PersonPåminnelser(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(PersonPåminnelser::class.java)
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection)
            .validate {
                it.demandAny("@event_name", listOf("minutt", "kjør_spock"))
            }
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        lagPåminnelser(context)
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
