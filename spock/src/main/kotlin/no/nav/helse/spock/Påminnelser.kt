package no.nav.helse.spock

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class Påminnelser(rapidsConnection: RapidsConnection,
                           private val dataSource: DataSource) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Påminnelser::class.java)
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
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
        log.info("håndterer tilstandsendring", keyValue("vedtaksperiodeId", packet["vedtaksperiodeId"].asText()))
        val event = TilstandsendringEventDto(packet)
        lagreTilstandsendring(dataSource, event)
        hentPåminnelser(dataSource).also {
            if (it.isNotEmpty()) {
                log.info("hentet ${it.size} påminnelser fra db")
                secureLogger.info("hentet ${it.size} påminnelser fra db")
            }
        }.onEach { påminnelse ->
            oppdaterPåminnelse(dataSource, påminnelse)
        }.map {
            it.fødselsnummer to it.toJson()
        }.onEach { (_, påminnelse) ->
            secureLogger.info("Produserer $påminnelse")
        }.forEach { (key, value) ->
            context.send(key, value)
        }
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
