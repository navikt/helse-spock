package no.nav.helse.spock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.sql.DataSource
import kotlin.math.abs

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
        TilstandsendringEventDto(packet).lagreTilstandsendring(dataSource)
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
            fun erSluttilstand(tilstand: String) = tilstand in listOf("AVSLUTTET", "TIL_INFOTRYGD")

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
                        0 -> LocalDateTime.now() // for å trigge at vi henter infotrygdhistorikk på nyopprettede personer asap
                        else -> defaultIntervall(endringstidspunkt)
                    }
                    "AVVENTER_SØKNAD_FERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
                    "AVVENTER_UFERDIG_GAP",
                    "AVVENTER_UFERDIG",
                    "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
                    "AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE",
                    "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE",
                    "AVVENTER_UFERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE",
                    "AVVENTER_SØKNAD_UFERDIG_GAP",
                    "AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP",
                    "AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP",
                    "UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP",
                    "UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE" -> defaultIntervall(endringstidspunkt)
                    "AVVENTER_VILKÅRSPRØVING",
                    "AVVENTER_VILKÅRSPRØVING_REVURDERING",
                    "AVVENTER_HISTORIKK_REVURDERING",
                    "AVVENTER_UTBETALINGSGRUNNLAG",
                    "AVVENTER_HISTORIKK" -> endringstidspunkt.plusHours(1)
                    "AVVENTER_GODKJENNING_REVURDERING",
                    "AVVENTER_ARBEIDSGIVERE",
                    "AVVENTER_ARBEIDSGIVERE_REVURDERING",
                    "AVVENTER_GODKJENNING" -> endringstidspunkt.plussTilfeldigeTimer(8, 12).withMinute((0..59).random()) // ca to/tre ganger i døgnet, et tilfeldig klokkslett
                    "TIL_UTBETALING",
                    "TIL_ANNULLERING",
                    "AVVENTER_SIMULERING_REVURDERING",
                    "AVVENTER_SIMULERING" -> OppdragUR.beregnPåminnelsetidspunkt(endringstidspunkt)
                    "START",
                    "AVVENTER_GJENNOMFØRT_REVURDERING", //Bør ikke påminnes, fordi den er avhengig av en periode som står i AVVENTER_GODKJENNING_REVURDERING
                    "REVURDERING_FEILET",
                    "UTBETALING_FEILET",
                    "AVSLUTTET_UTEN_UTBETALING",
                    "TIL_INFOTRYGD",
                    "AVSLUTTET" -> LocalDate.ofYearDay(9999, 1).atStartOfDay()
                    else -> {
                        sikkerLog.warn("Har ikke påminnelseregler for tilstand $tilstand")
                        defaultIntervall(endringstidspunkt)
                    }
                }

            // velger et tilfeldig klokkeslett mellom 18:00 og 05:59 _neste_ dag.
            // det betyr at om endringstidspunktet er 1. januar 23:59, og vi velger kl 01:00,
            // vil påminnelsen fyre av 3. januar 01:00, IKKE 2.januar 01:00
            internal fun defaultIntervall(endringstidspunkt: LocalDateTime): LocalDateTime {
                val min = endringstidspunkt.plusHours(12)
                val kveldstid = (18..23)
                val nattestid = (0..5)
                val tilfeldigTime = (nattestid.toList() + kveldstid.toList()).random()
                val diff = abs( min.hour - tilfeldigTime)
                val timer = if (tilfeldigTime in kveldstid) diff else (24 - diff)
                return min.plusHours(timer.toLong()).withMinute((0..59).random())
            }
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
