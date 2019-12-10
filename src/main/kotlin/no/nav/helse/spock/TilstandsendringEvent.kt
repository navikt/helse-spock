package no.nav.helse.spock

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.time.LocalDateTime

internal class TilstandsendringEvent private constructor(private val aktørId: String,
                                                         private val fødselsnummer: String,
                                                         private val organisasjonsnummer: String,
                                                         private val vedtaksperiodeId: String,
                                                         private val gjeldendeTilstand: String,
                                                         private val forrigeTilstand: String,
                                                         private val endringstidspunkt: LocalDateTime,
                                                         private val timeout: Long,
                                                         private val originalJson: String) {

    fun trengerPåminnelse() =
            endringstidspunkt.plusSeconds(timeout).isBefore(LocalDateTime.now())

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
                        "vedtaksperiodeId", "gjeldendeTilstand", "forrigeTilstand",
                        "endringstidspunkt", "timeout").forEach {
                    require(jsonNode.hasNonNull(it)) { "$it er påkrevd felt" }
                }

                TilstandsendringEvent(
                        aktørId = jsonNode["aktørId"].textValue(),
                        fødselsnummer = jsonNode["fødselsnummer"].textValue(),
                        organisasjonsnummer = jsonNode["organisasjonsnummer"].textValue(),
                        vedtaksperiodeId = jsonNode["vedtaksperiodeId"].textValue(),
                        gjeldendeTilstand = jsonNode["gjeldendeTilstand"].textValue(),
                        forrigeTilstand = jsonNode["forrigeTilstand"].textValue(),
                        endringstidspunkt = LocalDateTime.parse(jsonNode["endringstidspunkt"].textValue()),
                        timeout = jsonNode["timeout"].longValue(),
                        originalJson = json
                )
            }
        }
    }

    class Serde: org.apache.kafka.common.serialization.Serde<TilstandsendringEvent> {
        override fun deserializer(): Deserializer<TilstandsendringEvent> {
            return Deserializer { _, data ->
                fromJson(String(data))
            }
        }

        override fun serializer(): Serializer<TilstandsendringEvent> {
            return Serializer { _, event ->
                event.originalJson.toByteArray()
            }
        }
    }
}
