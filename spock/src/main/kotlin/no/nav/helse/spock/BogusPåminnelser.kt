package no.nav.helse.spock

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class BogusPåminnelser(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_ikke_funnet")
                it.requireKey("vedtaksperiodeId")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("kunne ikke forstå vedtaksperiode_ikke_funnet: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
        log.info("Sletter påminnelser for {} pga. vedtaksperiode ikke funnet", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        sikkerLogg.info("Sletter påminnelser for {} pga. vedtaksperiode ikke funnet", keyValue("vedtaksperiodeId", vedtaksperiodeId))
        slettPåminnelse(dataSource, vedtaksperiodeId)
    }
}
