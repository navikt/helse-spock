package no.nav.helse.spock

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime

internal class TilstandsendringEvent private constructor(private val aktørId: String,
                                                         private val fødselsnummer: String,
                                                         private val organisasjonsnummer: String,
                                                         private val vedtaksperiodeId: String,
                                                         private val gjeldendeTilstand: String,
                                                         private val forrigeTilstand: String,
                                                         private val endringstidspunkt: LocalDateTime,
                                                         private val timeout: Long) {

    fun trengerPåminnelse() =
            endringstidspunkt.plusSeconds(timeout).isAfter(LocalDateTime.now())

    fun nyeste(other: TilstandsendringEvent): TilstandsendringEvent {
        if (other.endringstidspunkt > this.endringstidspunkt) return other
        return this
    }

    companion object {
        private val objectMapper = ObjectMapper()

        fun grupper(event: TilstandsendringEvent): String {
            return event.vedtaksperiodeId
        }

        fun fromJson(json: String): TilstandsendringEvent {
            return objectMapper.readTree(json).let { jsonNode ->
                listOf("aktørId", "fødselsnummer", "organisasjonsnummer",
                        "vedtaksperiodeId", "currentState", "previousState",
                        "endringstidspunkt", "timeout").forEach {
                    require(jsonNode.hasNonNull(it))
                }

                TilstandsendringEvent(
                        aktørId = jsonNode["aktørId"].textValue(),
                        fødselsnummer = jsonNode["fødselsnummer"].textValue(),
                        organisasjonsnummer = jsonNode["organisasjonsnummer"].textValue(),
                        vedtaksperiodeId = jsonNode["vedtaksperiodeId"].textValue(),
                        gjeldendeTilstand = jsonNode["currentState"].textValue(),
                        forrigeTilstand = jsonNode["previousState"].textValue(),
                        endringstidspunkt = LocalDateTime.parse(jsonNode["endringstidspunkt"].textValue()),
                        timeout = jsonNode["timeout"].longValue()
                )
            }
        }
    }

}
