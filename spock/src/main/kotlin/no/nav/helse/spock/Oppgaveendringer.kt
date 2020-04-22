package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

class Oppgaveendringer(
    rapidsConnection: RapidsConnection,
    private val spesialistPåminnelseDao: SpesialistPåminnelseDao
) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "oppgave_oppdatert") }
            validate { it.requireKey("timeout", "spleisBehovId", "fødselsnummer") }
            validate { it.require("endringstidspunkt", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLog.error("kunne ikke forstå oppgave_oppdatert: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val referanse = packet["spleisbehovId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val timeout = packet["timeout"].longValue()
        val endringstidspunkt = packet["endringstidspunkt"].asLocalDateTime()
        spesialistPåminnelseDao.finnEllerOpprettPåminnelse(
                referanse = referanse,
                fødselsnummer = fødselsnummer,
                endringstidspunkt = endringstidspunkt,
                timeout = timeout
        )
    }
}
