package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

internal class TilstandsendringEvent(private val aktørId: String,
                                       private val fødselsnummer: String,
                                       private val organisasjonsnummer: String,
                                       private val vedtaksperiodeId: String,
                                       private val tilstand: String,
                                       private val timeout: Long,
                                       private val tidspunkt: LocalDateTime) {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fraJson(event: String): TilstandsendringEvent? {
            return try {
                objectMapper.readTree(event).let { jsonNode ->
                    val timeout = jsonNode.requiredLong("timeout")
                    TilstandsendringEvent(
                            aktørId = jsonNode.requiredString("aktørId"),
                            fødselsnummer = jsonNode.requiredString("fødselsnummer"),
                            organisasjonsnummer = jsonNode.requiredString("organisasjonsnummer"),
                            vedtaksperiodeId = jsonNode.requiredString("vedtaksperiodeId"),
                            tilstand = jsonNode.requiredString("gjeldendeTilstand"),
                            timeout = timeout,
                            tidspunkt = LocalDateTime.parse(jsonNode.requiredString("endringstidspunkt")).plusSeconds(timeout)
                    )
                }
            } catch (err: IllegalArgumentException) {
                println("kan ikke lese fra json pga.: ${err.message}")
                null
            }
        }

        private fun JsonNode.requiredLong(key: String) = required(key).longValue()
        private fun JsonNode.requiredString(key: String) = required(key).textValue()

        private fun JsonNode.required(key: String) =
                requireNotNull(this[key]?.takeUnless { it.isNull }) { "missing required value: $key" }
    }
}
