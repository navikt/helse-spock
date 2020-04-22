package no.nav.helse.spock

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.*

class SpesialistPåminnelseDto(
        internal val id: Long,
        internal val fødselsnummer: String,
        private val referanse: String,
        private val antallGangerPåminnet: Int,
        private val endringstidspunkt: LocalDateTime,
        private val timeout: Long
) {
    private val påminnelsestidspunkt = LocalDateTime.now()
    private val nestePåminnelsestidspunkt = påminnelsestidspunkt.plusSeconds(timeout)

    internal fun toJson() = JsonMessage.newMessage(mapOf(
        "@id" to UUID.randomUUID(),
        "@event_name" to "spesialist_påminnelse",
        "@opprettet" to påminnelsestidspunkt,
        "oppgaveendrettidspunkt" to endringstidspunkt,
        "referanse" to referanse,
        "antallGangerPåminnet" to antallGangerPåminnet,
        "påminnelsestidspunkt" to påminnelsestidspunkt,
        "nestePåminnelsestidspunkt" to nestePåminnelsestidspunkt
    )).toJson()

    override fun toString(): String {
        return "SpesialistPåminnelseDto(id='$id', fødselsnummer='$fødselsnummer', referanse='$referanse'," +
                "timeout=$timeout, endringstidspunkt=$endringstidspunkt, nestePåminnelsestidspunkt=$nestePåminnelsestidspunkt," +
                "antallGangerPåminnet=$antallGangerPåminnet)"
    }
}
