package no.nav.helse.spock

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class Tilstandsendringer(rapidsConnection: RapidsConnection,
                         private val dataSource: DataSource) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Tilstandsendringer::class.java)
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("timeout", "aktørId", "fødselsnummer",
                "organisasjonsnummer", "vedtaksperiodeId", "gjeldendeTilstand",
                "endringstidspunkt") }
        }.register(this)
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
        val endringstidspunkt = packet["endringstidspunkt"].asLocalDateTime()
        val originalJson = packet.toJson()
    }
}
