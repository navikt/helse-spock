package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.sql.DataSource

class Tilstandsendringer(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Tilstandsendringer::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "vedtaksperiode_endret") }
            validate {
                it.requireKey(
                    "aktørId", "fødselsnummer",
                    "organisasjonsnummer", "vedtaksperiodeId", "gjeldendeTilstand"
                )
            }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("kunne ikke forstå vedtaksperiode_endret: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val event = TilstandsendringEventDto(packet).also { it.lagreTilstandsendring(dataSource) }
        context.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "planlagt_påminnelse",
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "tilstand" to event.tilstand,
                    "endringstidspunkt" to event.endringstidspunkt,
                    "påminnelsetidspunkt" to event.nestePåminnelsetidspunkt(),
                    "er_avsluttet" to TilstandsendringEventDto.erSluttilstand(event.tilstand)
                )
            ).toJson()
        )
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
            fun erSluttilstand(tilstand: String) = tilstand in listOf(
                "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING",
                "AVSLUTTET_UTEN_UTBETALING",
                "AVSLUTTET",
                "TIL_INFOTRYGD"
            )

            fun nestePåminnelsetidspunkt(
                tilstand: String,
                endringstidspunkt: LocalDateTime,
                antallGangerPåminnet: Int
            ) =
                when (tilstand) {
                    "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
                    "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
                    "MOTTATT_SYKMELDING_FERDIG_GAP",
                    "MOTTATT_SYKMELDING_UFERDIG_GAP" -> when (antallGangerPåminnet) {
                        0 -> LocalDateTime.now()
                        else -> endringstidspunkt.plussTilfeldigeTimer(20, 24).plussTilfeldigeMinutter(60)
                    }
                    "AVVENTER_SØKNAD_FERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
                    "AVVENTER_UFERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE",
                    "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE",
                    "AVVENTER_UFERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_UFERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
                    "AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP",
                    "AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP",
                    "UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP",
                    "REVURDERING_FEILET",
                    "TIL_ANNULLERING",
                    "UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE" ->
                        defaultIntervall(endringstidspunkt)
                    "AVVENTER_VILKÅRSPRØVING",
                    "AVVENTER_ARBEIDSGIVERE",
                    "AVVENTER_ARBEIDSGIVERE_REVURDERING",
                    "AVVENTER_HISTORIKK_REVURDERING",
                    "AVVENTER_UTBETALINGSGRUNNLAG",
                    "AVVENTER_HISTORIKK" ->
                        if (endringstidspunkt.erHelg()) endringstidspunkt.plusHours(4)
                        else endringstidspunkt.plusHours(1)
                    "AVVENTER_GODKJENNING_REVURDERING",
                    "AVVENTER_GODKJENNING" -> endringstidspunkt.plusHours(24)
                    "TIL_UTBETALING",
                    "AVVENTER_SIMULERING_REVURDERING",
                    "AVVENTER_SIMULERING" -> OppdragUR.beregnPåminnelsetidspunkt(endringstidspunkt)
                    "START",
                    "UTBETALING_FEILET" -> LocalDate.ofYearDay(9999, 1).atStartOfDay()
                    else -> {
                        sikkerLog.warn("Har ikke påminnelseregler for tilstand $tilstand")
                        defaultIntervall(endringstidspunkt)
                    }
                }

            private fun defaultIntervall(endringstidspunkt: LocalDateTime) = (
                    if (endringstidspunkt.erHelg()) endringstidspunkt.plussTilfeldigeTimer(8, 12)
                    else endringstidspunkt.plussTilfeldigeTimer(5, 8)
                    ).plussTilfeldigeMinutter(59)
        }

        private object OppdragUR {
            // åpningstid oppdrag/ur (man-fre 06:00-20:59)
            private val åpningstiderOppdragUR = LocalTime.of(6, 0)..LocalTime.of(20, 59, 59)

            fun beregnPåminnelsetidspunkt(endringstidspunkt: LocalDateTime): LocalDateTime {
                // påminn hver time innenfor åpningstid, ellers vent til innenfor åpningstid
                if (innenforÅpningstid(endringstidspunkt)) return endringstidspunkt.plusHours(1)
                return nesteÅpningsdagtidspunkt(endringstidspunkt)
            }

            // samme dag om vi er før åpningstid (og ukedag), neste mandag hvis ikke
            private fun nesteÅpningsdagtidspunkt(endringstidspunkt: LocalDateTime): LocalDateTime {
                val nesteÅpningsdag =
                    if (endringstidspunkt.erHelg() || etterStengetid(endringstidspunkt)) endringstidspunkt.nesteUkedag()
                    else endringstidspunkt.toLocalDate()
                // spre påminnelsene litt utover morgentimene
                return nesteÅpningsdag.atTime(åpningstiderOppdragUR.start).plussTilfeldigeTimer(0, 1)
                    .plussTilfeldigeMinutter(59)
            }

            private fun LocalDateTime.nesteUkedag() = this.plusDays(
                when (this.dayOfWeek) {
                    FRIDAY -> 3
                    SATURDAY -> 2
                    else -> 1
                }
            ).toLocalDate()

            private fun etterStengetid(endringstidspunkt: LocalDateTime) =
                endringstidspunkt.toLocalTime() > åpningstiderOppdragUR.endInclusive

            private fun innenforÅpningstid(endringstidspunkt: LocalDateTime) =
                !endringstidspunkt.erHelg() && endringstidspunkt.toLocalTime() in åpningstiderOppdragUR
        }
    }
}

private fun LocalDateTime.erHelg() = this.dayOfWeek == SATURDAY || this.dayOfWeek == SUNDAY
private fun LocalDateTime.plussTilfeldigeTimer(min: Int, max: Int) = this.plusHours((min..max).random().toLong())
private fun LocalDateTime.plussTilfeldigeMinutter(minutter: Int) =
    if (minutter < 1) this else this.plusMinutes((1..minutter).random().toLong())
