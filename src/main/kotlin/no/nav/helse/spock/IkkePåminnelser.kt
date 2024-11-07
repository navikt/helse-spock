package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import net.logstash.logback.argument.StructuredArguments.keyValue
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
                it.requireKey("fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId", "tilstand")
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("kunne ikke forstå vedtaksperiode_ikke_påminnet: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
        val tilstand = packet["tilstand"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        lagreTilstandsendring(
            dataSource,
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
