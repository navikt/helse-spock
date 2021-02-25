package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

class OppgavePåminnelser(
        rapidsConnection: RapidsConnection,
        private val oppgaveMakstidPåminnelseDao: OppgaveMakstidPåminnelseDao
) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "oppgave_opprettet") }
            validate { it.requireKey("oppgaveId", "@id", "fødselsnummer", "status") }
            validate { it.require("makstid", JsonNode::asLocalDateTime) }
        }.register(this)

        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "oppgave_oppdatert") }
            validate { it.requireKey("oppgaveId", "@id", "fødselsnummer", "status") }
            validate { it.require("makstid", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("kunne ikke forstå oppgave_opprettet: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["@id"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val oppgaveId = packet["oppgaveId"].longValue()
        val makstid = packet["makstid"].asLocalDateTime()
        val status = packet["status"].asText()
        oppgaveMakstidPåminnelseDao.behandlePåminnelse(
                oppgaveId = oppgaveId,
                fødselsnummer = fødselsnummer,
                makstid = makstid,
                status = status,
                eventId = eventId
        )
    }
}
