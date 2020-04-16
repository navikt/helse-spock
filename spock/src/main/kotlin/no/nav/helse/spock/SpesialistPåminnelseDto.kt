package no.nav.helse.spock

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
    val påminnelsestidspunkt = LocalDateTime.now()
    val nestePåminnelsestidspunkt = påminnelsestidspunkt.plusSeconds(timeout)

    internal fun toJson() = objectMapper.writeValueAsString(
            mapOf(
                    "@id" to UUID.randomUUID().toString(),
                    "@event_name" to "spesialistpåminnelse",
                    "@opprettet" to påminnelsestidspunkt,
                    "oppgaveendrettidspunkt" to endringstidspunkt,
                    "referanse" to referanse,
                    "antallGangerPåminnet" to antallGangerPåminnet,
                    "påminnelsestidspunkt" to påminnelsestidspunkt,
                    "nestePåminnelsestidspunkt" to nestePåminnelsestidspunkt
            ))

    override fun toString(): String {
        return "SpesialistPåminnelseDto(id='$id', fødselsnummer='$fødselsnummer', referanse='$referanse'," +
                "timeout=$timeout, endringstidspunkt=$endringstidspunkt, nestePåminnelsestidspunkt=$nestePåminnelsestidspunkt," +
                "antallGangerPåminnet=$antallGangerPåminnet)"
    }
}