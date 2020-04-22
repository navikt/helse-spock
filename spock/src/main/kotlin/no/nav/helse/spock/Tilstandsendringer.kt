package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class Tilstandsendringer(rapidsConnection: RapidsConnection,
                         private val dataSource: DataSource) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Tilstandsendringer::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("timeout", "aktørId", "fødselsnummer",
                "organisasjonsnummer", "vedtaksperiodeId", "gjeldendeTilstand") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLog.error("kunne ikke forstå vedtaksperiode_endret: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val event = TilstandsendringEventDto(packet)
        lagreTilstandsendring(dataSource, event)
    }

    class TilstandsendringEventDto(packet: JsonMessage) {
        val aktørId = packet["aktørId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
        val tilstand = packet["gjeldendeTilstand"].asText()
        val timeout = packet["timeout"].longValue()
        val endringstidspunkt = packet["@opprettet"].asLocalDateTime()
        val originalJson = packet.toJson()
    }
}
