package no.nav.helse.spock

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class Forkastelser(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_forkastet")
                it.requireKey("vedtaksperiodeId")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLogg.error("kunne ikke forstå vedtaksperiode_forkastet: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
        log.info("Sletter påminnelser for {} pga. forkastelse", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        sikkerLogg.info("Sletter påminnelser for {} pga. forkastelse", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        slettPåminnelse(dataSource, vedtaksperiodeId)
    }
}
