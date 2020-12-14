package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.sql.DataSource

class Tilstandsendringer(rapidsConnection: RapidsConnection,
                         private val dataSource: DataSource) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Tilstandsendringer::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("aktørId", "fødselsnummer",
                "organisasjonsnummer", "vedtaksperiodeId", "gjeldendeTilstand") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLog.error("kunne ikke forstå vedtaksperiode_endret: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val event = TilstandsendringEventDto(packet).also { it.lagreTilstandsendring(dataSource) }
        context.send(JsonMessage.newMessage(mapOf(
            "@event_name" to "planlagt_påminnelse",
            "@opprettet" to LocalDateTime.now(),
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "tilstand" to event.tilstand,
            "endringstidspunkt" to event.endringstidspunkt,
            "påminnelsetidspunkt" to event.nestePåminnelsetidspunkt(),
            "er_avsluttet" to TilstandsendringEventDto.erSluttilstand(event.tilstand)
        )).toJson())
    }

    class TilstandsendringEventDto(packet: JsonMessage) {
        val aktørId = packet["aktørId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
        val tilstand = packet["gjeldendeTilstand"].asText()
        val endringstidspunkt = packet["@opprettet"].asLocalDateTime()
        val originalJson = packet.toJson()

        fun lagreTilstandsendring(dataSource: DataSource) {
            lagreTilstandsendring(
                dataSource,
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId,
                tilstand,
                endringstidspunkt,
                nestePåminnelsetidspunkt(),
                originalJson
            )
        }
        fun nestePåminnelsetidspunkt() = nestePåminnelsetidspunkt(tilstand, endringstidspunkt, 0)

        companion object {
            private val åpningstiderOppdragUR = LocalTime.of(7, 0)..LocalTime.of(19, 59, 59)

            fun erSluttilstand(tilstand: String) = tilstand in listOf(
                "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING",
                "AVSLUTTET", "TIL_INFOTRYGD"
            )

            fun nestePåminnelsetidspunkt(tilstand: String, endringstidspunkt: LocalDateTime, antallGangerPåminnet: Int) =
                when (tilstand) {
                    "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
                    "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
                    "MOTTATT_SYKMELDING_FERDIG_GAP",
                    "MOTTATT_SYKMELDING_UFERDIG_GAP",
                    "AVVENTER_SØKNAD_FERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
                    "AVVENTER_UFERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE",
                    "AVVENTER_UFERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_UFERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_FERDIG_GAP" -> {
                        when (endringstidspunkt.dayOfWeek) {
                            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY ->
                                endringstidspunkt.plusHours(12)
                            else ->
                                endringstidspunkt.plusHours(6)
                        }.plussTilfeldigeMinutter(60)
                    }
                    "AVVENTER_GAP",
                    "AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD",
                    "AVVENTER_VILKÅRSPRØVING_GAP",
                    "AVVENTER_ARBEIDSGIVERE",
                    "AVVENTER_HISTORIKK" -> {
                        when (endringstidspunkt.dayOfWeek) {
                            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY ->
                                endringstidspunkt.plusHours(4)
                            else ->
                                endringstidspunkt.plusHours(1)
                        }
                    }
                    "AVVENTER_GODKJENNING" -> endringstidspunkt.plusHours(24)
                    "TIL_UTBETALING",
                    "AVVENTER_SIMULERING" -> {
                        // påminn hver time innenfor åpningstid (man-fre 07:00-19:59), ellers vent til innenfor åpningstid
                        val klslett = endringstidspunkt.toLocalTime()
                        when (endringstidspunkt.dayOfWeek) {
                            DayOfWeek.SATURDAY -> åpningstiderOppdragUR.start.atDate(endringstidspunkt.plusDays(2).toLocalDate())
                            DayOfWeek.SUNDAY -> åpningstiderOppdragUR.start.atDate(endringstidspunkt.plusDays(1).toLocalDate())
                            DayOfWeek.FRIDAY -> {
                                when {
                                    klslett > åpningstiderOppdragUR.endInclusive -> åpningstiderOppdragUR.start.atDate(endringstidspunkt.plusDays(3).toLocalDate())
                                    klslett < åpningstiderOppdragUR.start -> åpningstiderOppdragUR.start.atDate(endringstidspunkt.toLocalDate())
                                    else -> endringstidspunkt.plusHours(1)
                                }
                            }
                            else -> {
                                when {
                                    klslett > åpningstiderOppdragUR.endInclusive -> åpningstiderOppdragUR.start.atDate(endringstidspunkt.plusDays(1).toLocalDate())
                                    klslett < åpningstiderOppdragUR.start -> åpningstiderOppdragUR.start.atDate((endringstidspunkt.toLocalDate()))
                                    else -> endringstidspunkt.plusHours(1)
                                }
                            }
                        }
                    }
                    "START",
                    "UTBETALING_FEILET",
                    "AVSLUTTET_UTEN_UTBETALING",
                    "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING" -> LocalDate.ofYearDay(9999, 1).atStartOfDay()
                    else -> LocalDate.ofYearDay(9999, 1).atStartOfDay()
                }
        }
    }
}

private fun LocalDateTime.plussTilfeldigeMinutter(minutter: Int) = if(minutter < 1) this else this.plusMinutes((1..minutter).random().toLong())
