package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

class Oppgaveendringer(rapidsConnection: RapidsConnection,
                       private val spesialistPåminnelseDao: SpesialistPåminnelseDao) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "oppgave_opprettet") }
            validate { it.requireKey("timeout", "spleisBehovId", "fødselsnummer") }
            validate { it.require("endringstidspunkt", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val referanse = packet["spleisBehovId"].asText()
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
