package no.nav.helse.spock

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import javax.sql.DataSource

internal class IkkePåminnelser(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource,
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "vedtaksperiode_ikke_påminnet") }
                validate {
                    it.requireKey("fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId", "tilstand")
                    it.require("@opprettet", JsonNode::asLocalDateTime)
                }
            }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerLogg.error("kunne ikke forstå vedtaksperiode_ikke_påminnet: ${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asString()
        val organisasjonsnummer = packet["organisasjonsnummer"].asString()
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asString()
        val tilstand = packet["tilstand"].asString()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        lagreTilstandsendring(
            dataSource,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            tilstand,
            opprettet,
            Tilstandsendringer.TilstandsendringEventDto.nestePåminnelsetidspunkt(tilstand, opprettet, 0),
            packet.toJson(),
        )
        log.info(
            "Setter tilstand=$tilstand for {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
        )
        sikkerLogg.info(
            "Setter tilstand=$tilstand for {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
        )
    }
}
