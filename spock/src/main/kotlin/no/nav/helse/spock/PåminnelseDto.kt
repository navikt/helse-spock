package no.nav.helse.spock

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class PåminnelseDto(val id: String,
                    val aktørId: String,
                    val fødselsnummer: String,
                    val organisasjonsnummer: String,
                    val vedtaksperiodeId: String,
                    val tilstand: String,
                    val endringstidspunkt: LocalDateTime,
                    val antallGangerPåminnet: Int) {

    val påminnelsestidspunkt = LocalDateTime.now()
    val nestePåminnelsetidspunkt = Tilstandsendringer.TilstandsendringEventDto.nestePåminnelsetidspunkt(tilstand, påminnelsestidspunkt, antallGangerPåminnet)
    // forventet tid i tilstand er tiden mellom endringstidspunktet og påminnelse nr 1
    val timeout = ChronoUnit.SECONDS.between(
        endringstidspunkt,
        Tilstandsendringer.TilstandsendringEventDto.nestePåminnelsetidspunkt(tilstand, endringstidspunkt, 0)
    )

    internal fun toJson() = JsonMessage.newMessage(mapOf(
        "@id" to UUID.randomUUID(),
        "@event_name" to "påminnelse",
        "@opprettet" to påminnelsestidspunkt,
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "tilstand" to tilstand,
        "tilstandsendringstidspunkt" to endringstidspunkt,
        "antallGangerPåminnet" to antallGangerPåminnet,
        "påminnelsestidspunkt" to påminnelsestidspunkt,
        "nestePåminnelsestidspunkt" to nestePåminnelsetidspunkt,
        "timeout_første_påminnelse" to timeout
    )).toJson()

    override fun toString(): String {
        return "PåminnelseDto(id='$id', aktørId='$aktørId', fødselsnummer='$fødselsnummer', " +
                "organisasjonsnummer='$organisasjonsnummer', vedtaksperiodeId='$vedtaksperiodeId', " +
                "tilstand='$tilstand', timeout=$timeout, endringstidspunkt=$endringstidspunkt, " +
                "nestePåminnelsestidspunkt=$nestePåminnelsetidspunkt, antallGangerPåminnet=$antallGangerPåminnet)"
    }
}
