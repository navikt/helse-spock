package no.nav.helse.spock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.*

class PåminnelseDto(val id: String,
                    val aktørId: String,
                    val fødselsnummer: String,
                    val organisasjonsnummer: String,
                    val vedtaksperiodeId: String,
                    val tilstand: String,
                    val timeout: Long,
                    val endringstidspunkt: LocalDateTime,
                    val antallGangerPåminnet: Int) {

    val påminnelsestidspunkt = LocalDateTime.now()
    val nestePåminnelsestidspunkt = påminnelsestidspunkt.plusSeconds(timeout)

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    internal fun toJson() = objectMapper.writeValueAsString(
        mapOf(
            "@id" to UUID.randomUUID().toString(),
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
            "nestePåminnelsestidspunkt" to nestePåminnelsestidspunkt
        ))

    override fun toString(): String {
        return "PåminnelseDto(id='$id', aktørId='$aktørId', fødselsnummer='$fødselsnummer', " +
                "organisasjonsnummer='$organisasjonsnummer', vedtaksperiodeId='$vedtaksperiodeId', " +
                "tilstand='$tilstand', timeout=$timeout, endringstidspunkt=$endringstidspunkt, " +
                "nestePåminnelsestidspunkt=$nestePåminnelsestidspunkt, antallGangerPåminnet=$antallGangerPåminnet)"
    }
}
