package no.nav.helse.spock.opprydding_dev

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.sykepenger.libs.logging.MdcKey
import no.nav.sykepenger.libs.logging.loggInfo
import no.nav.sykepenger.libs.logging.medMdc
import javax.sql.DataSource

internal class SlettPersonRiver(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "slett_person") }
                validate {
                    it.requireKey("@id", "fødselsnummer")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asString()
        medMdc(MdcKey.IDENTITETSNUMMER to fødselsnummer) {
            loggInfo("Mottok og tolket slett_person-melding", "melding" to packet.toJson())
            sessionOf(dataSource).use { session ->
                session.transaction { tx -> slettPerson(tx, fødselsnummer) }
            }

            context.publish(fødselsnummer, lagPersonSlettet(fødselsnummer))
            loggInfo("Ferdig med slett_person-melding")
        }
    }

    private fun slettPerson(
        tx: TransactionalSession,
        fødselsnummer: String,
    ) {
        listOf("paminnelse", "utbetaling").forEach { table ->
            tx
                .run(
                    queryOf(
                        "DELETE FROM $table WHERE fnr = :fnr",
                        mapOf("fnr" to fødselsnummer),
                    ).asUpdate,
                ).also { loggInfo("Slettet $it rader i tabellen $table") }
        }
        tx
            .run(
                queryOf(
                    "DELETE FROM person WHERE fnr = :fnr",
                    mapOf("fnr" to fødselsnummer.toLong()),
                ).asUpdate,
            ).also { loggInfo("Slettet $it rader i tabellen person") }
    }

    private fun lagPersonSlettet(fødselsnummer: String): String =
        """
        {
          "@event_name": "person_slettet",
          "fødselsnummer": "$fødselsnummer"
        }
        """.trimIndent()
}
