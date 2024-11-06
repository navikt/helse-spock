package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class UtbetalingEndret(rapidsConnection: RapidsConnection,
                         private val dataSource: DataSource) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "utbetaling_endret")
                it.rejectValue("type", "FERIEPENGER")
                it.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer", "utbetalingId")
                it.requireAny("type", listOf("UTBETALING", "ANNULLERING", "ETTERUTBETALING", "REVURDERING"))
                it.requireAny("gjeldendeStatus", listOf(
                    "IKKE_UTBETALT", "FORKASTET", "IKKE_GODKJENT",
                    "GODKJENT", "OVERFØRT", "AVVENTER_KVITTERINGER",
                    "AVVENTER_ARBEIDSGIVERKVITTERING", "AVVENTER_PERSONKVITTERING",
                    "UTBETALT", "ANNULLERT", "GODKJENT_UTEN_UTBETALING"
                ))
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("kunne ikke forstå utbetaling_endret: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        UtbetalingPåminnelser.Utbetalingpåminnelse.opprett(packet, dataSource)
    }
}
