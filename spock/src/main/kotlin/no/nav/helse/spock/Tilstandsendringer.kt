package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
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
        val event = TilstandsendringEventDto(packet)
        lagreTilstandsendring(dataSource, event)
    }

    class TilstandsendringEventDto(packet: JsonMessage) {
        val aktørId = packet["aktørId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
        val tilstand = packet["gjeldendeTilstand"].asText()
        val endringstidspunkt = packet["@opprettet"].asLocalDateTime()
        val originalJson = packet.toJson()

        fun erSluttilstand() = tilstand in listOf(
            "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING",
            "AVSLUTTET", "TIL_INFOTRYGD"
        )

        fun nestePåminnelsetidspunkt() = nestePåminnelsetidspunkt(tilstand, endringstidspunkt, 0)

        companion object {
            private val åpningstiderOppdragUR = LocalTime.of(7, 0)..LocalTime.of(19, 59, 59)

            fun nestePåminnelsetidspunkt(tilstand: String, endringstidspunkt: LocalDateTime, antallGangerPåminnet: Int) =
                when (tilstand) {
                    "START",
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
                    "AVVENTER_INNTEKTSMELDING_FERDIG_GAP" -> endringstidspunkt.plusDays(30)
                    "AVVENTER_GAP",
                    "AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD",
                    "AVVENTER_VILKÅRSPRØVING_GAP",
                    "AVVENTER_HISTORIKK" -> endringstidspunkt.plusHours(1)
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
                    "AVVENTER_GODKJENNING" -> {
                        // vent tre virkedager med initiel påminnelse, 1 time pr. påminnelse deretter
                        if (antallGangerPåminnet > 1) endringstidspunkt.plusHours(1)
                        else {
                            val dager = 3L + when (endringstidspunkt.dayOfWeek) {
                                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY -> 2
                                DayOfWeek.SATURDAY -> 1
                                else -> 0
                            }
                            endringstidspunkt.plusDays(dager)
                        }
                    }
                    "TIL_UTBETALING",
                    "UTBETALING_FEILET",
                    "AVSLUTTET_UTEN_UTBETALING",
                    "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING" -> LocalDateTime.MAX
                    else -> LocalDateTime.MIN
                }
        }
    }
}
