package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class IkkePåminnelser(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_ikke_påminnet")
                it.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId", "tilstand")
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.error("kunne ikke forstå vedtaksperiode_ikke_påminnet: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
        val tilstand = packet["tilstand"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        lagreTilstandsendring(
            dataSource,
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            tilstand,
            opprettet,
            Tilstandsendringer.TilstandsendringEventDto.nestePåminnelsetidspunkt(tilstand, opprettet, 0),
            packet.toJson()
        )
        log.info(
            "Setter tilstand=$tilstand for {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId)
        )
        sikkerLogg.info(
            "Setter tilstand=$tilstand for {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId)
        )
    }
}
