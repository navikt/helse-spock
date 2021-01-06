package no.nav.helse.spock

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.*

class OppgaveMakstidPåminnelseDto(
        internal val oppgaveId: Long,
        internal val fødselsnummer: String,
) {
    internal fun toJson() = JsonMessage.newMessage(mapOf(
            "@event_name" to "påminnelse_oppgave_makstid",
            "@id" to UUID.randomUUID(),
            "oppgaveId" to oppgaveId,
            "fødselsnummer" to fødselsnummer
    )).toJson()
}
