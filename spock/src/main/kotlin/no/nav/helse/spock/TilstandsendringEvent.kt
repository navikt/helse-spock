package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

internal fun MutableList<TilstandsendringEvent>.håndter(event: TilstandsendringEvent) = TilstandsendringEvent.håndter(this, event)

// Understands when a state change need reminding
internal class TilstandsendringEvent(private val aktørId: String,
                                     private val fødselsnummer: String,
                                     private val organisasjonsnummer: String,
                                     private val vedtaksperiodeId: String,
                                     private val tilstand: String,
                                     private val timeout: Long,
                                     private val endringstidspunkt: LocalDateTime) {

    private val påminnelse = Påminnelse()

    fun addWhenDue(påminnelser: MutableList<Påminnelse>): Boolean {
        return påminnelse.addWhenDue(påminnelser)
    }

    fun nyeste(other: TilstandsendringEvent?): TilstandsendringEvent {
        if (other == null) return this
        require(this.vedtaksperiodeId == other.vedtaksperiodeId) { "Vedtaksperiode må være lik" }
        if (this.endringstidspunkt < other.endringstidspunkt) return other
        return this
    }

    internal inner class Påminnelse {
        private var nestePåminnelsestidspunkt = endringstidspunkt.plusSeconds(timeout)
        private var antallGangerPåminnet = 0

        internal fun addWhenDue(påminnelser: MutableList<Påminnelse>): Boolean {
            if (LocalDateTime.now() < this.nestePåminnelsestidspunkt) return false
            påminnelser.add(this)
            antallGangerPåminnet++
            nestePåminnelsestidspunkt = LocalDateTime.now().plusSeconds(timeout)
            return true
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

        internal fun infoLogg() = "tilstand: $tilstand, vedtaksperiodeId: $vedtaksperiodeId"
    }

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        internal fun håndter(events: MutableList<TilstandsendringEvent>, event: TilstandsendringEvent) {
            val index = events.indexOfFirst { it.vedtaksperiodeId == event.vedtaksperiodeId }

            val other = if (index >= 0) events.removeAt(index) else null
            val beste = event.nyeste(other)

            if (beste.timeout <= 0) return
            events.add(beste)
        }

        fun fraJson(event: String): TilstandsendringEvent? {
            return try {
                objectMapper.readTree(event).let { jsonNode ->
                    jsonNode["@event_name"]?.asText()?.also {
                        require(it == "vedtaksperiode_endret")
                    }
                    val timeout = jsonNode.requiredLong("timeout")
                    TilstandsendringEvent(
                            aktørId = jsonNode.requiredString("aktørId"),
                            fødselsnummer = jsonNode.requiredString("fødselsnummer"),
                            organisasjonsnummer = jsonNode.requiredString("organisasjonsnummer"),
                            vedtaksperiodeId = jsonNode.requiredString("vedtaksperiodeId"),
                            tilstand = jsonNode.requiredString("gjeldendeTilstand"),
                            timeout = timeout,
                            endringstidspunkt = LocalDateTime.parse(jsonNode.requiredString("endringstidspunkt"))
                    )
                }
            } catch (err: IllegalArgumentException) {
                println("kan ikke lese fra json pga.: ${err.message}")
                null
            }
        }

        private fun JsonNode.requiredLong(key: String) = requiredKey(key).longValue()
        private fun JsonNode.requiredString(key: String) = requiredKey(key).textValue()

        private fun JsonNode.requiredKey(key: String) =
                requireNotNull(this[key]?.takeUnless { it.isNull }) { "missing required value: $key" }
    }
}
