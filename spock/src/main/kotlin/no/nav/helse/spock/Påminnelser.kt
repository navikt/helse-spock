package no.nav.helse.spock

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import javax.sql.DataSource

class Påminnelser(rapidsConnection: RapidsConnection,
                  private val dataSource: DataSource,
                  schedule: Duration) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Påminnelser::class.java)
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
        val påminnelser = hentPåminnelser(dataSource)
        if (påminnelser.isEmpty()) return
        log.info("hentet ${påminnelser.size} påminnelser fra db")
        secureLogger.info("hentet ${påminnelser.size} påminnelser fra db")
        påminnelser.map {
            it.fødselsnummer to it.toJson()
        }.onEach { (_, påminnelse) ->
            secureLogger.info("Produserer $påminnelse")
        }.forEach { (key, value) ->
            context.send(key, value)
        }
    }
}
