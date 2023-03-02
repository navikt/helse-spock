package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.rapids_rivers.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import javax.sql.DataSource

class UtbetalingEndret(rapidsConnection: RapidsConnection,
                         private val dataSource: DataSource) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.rejectValue("type", "FERIEPENGER")
                it.demandValue("@event_name", "utbetaling_endret")
                it.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer", "utbetalingId")
                it.requireAny("type", listOf("UTBETALING", "ANNULLERING", "ETTERUTBETALING", "REVURDERING"))
                it.requireAny("gjeldendeStatus", listOf(
                    "IKKE_UTBETALT", "FORKASTET", "IKKE_GODKJENT",
                    "GODKJENT", "OVERFØRT", "AVVENTER_KVITTERINGER",
                    "AVVENTER_ARBEIDSGIVERKVITTERING", "AVVENTER_PERSONKVITTERING",
                    "UTBETALT", "ANNULLERT", "GODKJENT_UTEN_UTBETALING"
                ))
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("kunne ikke forstå utbetaling_endret: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        UtbetalingPåminnelser.Utbetalingpåminnelse.opprett(packet, dataSource)
    }
}
