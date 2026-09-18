package no.nav.helse.spock

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

data class PåminnelseDto(
    val id: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: String,
    val tilstand: String,
    val endringstidspunkt: LocalDateTime,
    val antallGangerPåminnet: Int,
    val ønskerReberegning: Boolean = false
) {

    val påminnelsestidspunkt = LocalDateTime.now(ZoneId.of("Europe/Oslo"))
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
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
        "yrkesaktivitetstype" to when (val type = organisasjonsnummer.uppercase()) {
            "ARBEIDSLEDIG",
            "SELVSTENDIG",
            "FRILANS" -> type
            else -> "ARBEIDSTAKER"
        },
        "vedtaksperiodeId" to vedtaksperiodeId,
        "tilstand" to tilstand,
        "tilstandsendringstidspunkt" to endringstidspunkt,
        "antallGangerPåminnet" to antallGangerPåminnet,
        "påminnelsestidspunkt" to påminnelsestidspunkt,
        "nestePåminnelsestidspunkt" to nestePåminnelsetidspunkt,
        "flagg" to when (ønskerReberegning) {
            true -> listOf("ønskerReberegning")
            false -> emptyList()
        },
        "timeout_første_påminnelse" to timeout
    )).toJson()
}
