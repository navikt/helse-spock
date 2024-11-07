package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.spock.Tilstandsendringer.TilstandsendringEventDto.Companion.nestePåminnelsetidspunkt
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

internal class PersonAvstemminger(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "person_avstemt")
                it.requireKey("fødselsnummer")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.require("@id", JsonNode::asUUID)
                it.requireArray("arbeidsgivere") {
                    requireKey("organisasjonsnummer")
                    requireArray("vedtaksperioder") {
                        requireKey("id", "tilstand")
                        require("oppdatert", JsonNode::asLocalDateTime)
                    }
                    requireArray("forkastedeVedtaksperioder") {
                        requireKey("id", "tilstand")
                        require("oppdatert", JsonNode::asLocalDateTime)
                    }
                    requireArray("utbetalinger") {
                        requireKey("id", "type", "status")
                        require("oppdatert", JsonNode::asLocalDateTime)
                    }
                }
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("kunne ikke forstå person_avstemt: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        sikkerLogg.info("Avstemmer spock mot resultat fra spleis sendt $opprettet", kv("fødselsnummer", fødselsnummer))

        packet["arbeidsgivere"].forEach { arbeidsgiver ->
            val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                val tilstand = vedtaksperiode.path("tilstand").asText()
                val endringstidspunkt = vedtaksperiode.path("oppdatert").asLocalDateTime()
                lagreTilstandsendring(
                    dataSource,
                    fødselsnummer,
                    organisasjonsnummer,
                    vedtaksperiode.path("id").asText(),
                    tilstand,
                    endringstidspunkt,
                    nestePåminnelsetidspunkt(tilstand, endringstidspunkt, 0),
                    vedtaksperiode.toString()
                )
            }
            arbeidsgiver.path("forkastedeVedtaksperioder").forEach { vedtaksperiode ->
                slettPåminnelse(dataSource, vedtaksperiode.path("id").asText())
            }
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                UtbetalingPåminnelser.Utbetalingpåminnelse(
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    utbetalingId = utbetaling.path("id").asUUID(),
                    type = utbetaling.path("type").asText(),
                    status = utbetaling.path("status").asText(),
                    endringstidspunkt = utbetaling.path("oppdatert").asLocalDateTime(),
                    data = utbetaling.toString()
                ).lagre(dataSource, overskriv = true)
            }
        }

        sikkerLogg.info("Avstemming utført", kv("fødselsnummer", fødselsnummer))
    }

}

private fun JsonNode.asUUID() = UUID.fromString(asText())
