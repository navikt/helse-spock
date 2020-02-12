package no.nav.helse.spock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

class PåminnelseDto(val id: String,
                    val aktørId: String,
                    val fødselsnummer: String,
                    val organisasjonsnummer: String,
                    val vedtaksperiodeId: String,
                    val tilstand: String,
                    val timeout: Long,
                    val endringstidspunkt: LocalDateTime,
                    val nestePåminnelsestidspunkt: LocalDateTime,
                    val antallGangerPåminnet: Int) {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    internal fun toJson() = objectMapper.writeValueAsString(
        mapOf(
            "@event_name" to "påminnelse",
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "tilstand" to tilstand,
            "tilstandsendringstidspunkt" to endringstidspunkt,
            "antallGangerPåminnet" to antallGangerPåminnet,
            "påminnelsestidspunkt" to LocalDateTime.now(),
            "nestePåminnelsestidspunkt" to nestePåminnelsestidspunkt
        ))

    override fun toString(): String {
        return "PåminnelseDto(id='$id', aktørId='$aktørId', fødselsnummer='$fødselsnummer', " +
                "organisasjonsnummer='$organisasjonsnummer', vedtaksperiodeId='$vedtaksperiodeId', " +
                "tilstand='$tilstand', timeout=$timeout, endringstidspunkt=$endringstidspunkt, " +
                "nestePåminnelsestidspunkt=$nestePåminnelsestidspunkt, antallGangerPåminnet=$antallGangerPåminnet)"
    }
}
