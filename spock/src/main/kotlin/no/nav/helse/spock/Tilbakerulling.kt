package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class Tilbakerulling(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger("tilbakerulling")
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "person_rullet_tilbake")
                it.requireArray("vedtaksperioderSlettet")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.error("kunne ikke forstå person_rullet_tilbake: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val tilbakerullingEventDto = TilbakerullingEventDto(packet)
        tilbakerullingEventDto.vedtakperioderSlettet.forEach { vedtaksperiodeId ->
            log.info("Sletter påminnelser for {}", keyValue("vedtaksperiodeId", vedtaksperiodeId))
            sikkerLogg.info("Sletter påminnelser for {}", keyValue("vedtaksperiodeId", vedtaksperiodeId))
            slettPåminnelse(dataSource, vedtaksperiodeId)
        }
    }

    internal class TilbakerullingEventDto(packet: JsonMessage) {
        val vedtakperioderSlettet: List<String> = packet["vedtaksperioderSlettet"].map(JsonNode::asText)
    }
}