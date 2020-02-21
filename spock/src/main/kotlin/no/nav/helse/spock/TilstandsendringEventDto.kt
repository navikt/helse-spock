package no.nav.helse.spock

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

class TilstandsendringEventDto private constructor(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: String,
    val tilstand: String,
    val timeout: Long,
    val endringstidspunkt: LocalDateTime,
    val originalJson: String) {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)


        fun fraJson(event: String): TilstandsendringEventDto? {
            return try {
                objectMapper.readTree(event).let { jsonNode ->
                    require(jsonNode.path("@event_name").asText() == "vedtaksperiode_endret")
                    val timeout = jsonNode.requiredLong("timeout")
                    TilstandsendringEventDto(
                        aktørId = jsonNode.requiredString("aktørId"),
                        fødselsnummer = jsonNode.requiredString("fødselsnummer"),
                        organisasjonsnummer = jsonNode.requiredString("organisasjonsnummer"),
                        vedtaksperiodeId = jsonNode.requiredString("vedtaksperiodeId"),
                        tilstand = jsonNode.requiredString("gjeldendeTilstand"),
                        timeout = timeout,
                        endringstidspunkt = LocalDateTime.parse(jsonNode.requiredString("endringstidspunkt")),
                        originalJson = event
                    )
                }
            } catch (err: JsonProcessingException) {
                null
            } catch (err: IllegalArgumentException) {
                null
            }
        }

        private fun JsonNode.requiredLong(key: String) = requiredKey(key).longValue()
        private fun JsonNode.requiredString(key: String) = requiredKey(key).textValue()

        private fun JsonNode.requiredKey(key: String) =
            requireNotNull(this[key]?.takeUnless { it.isNull }) { "missing required value: $key" }
    }
}
