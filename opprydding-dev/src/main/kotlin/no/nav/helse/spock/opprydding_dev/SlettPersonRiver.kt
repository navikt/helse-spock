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

        sessionOf(dataSource).use { session ->
            session.transaction { tx -> slettPerson(tx, fødselsnummer) }
        }

        context.publish(fødselsnummer, lagPersonSlettet(fødselsnummer))
    }

    private fun slettPerson(
        tx: TransactionalSession,
        fødselsnummer: String,
    ) {
        listOf("paminnelse", "utbetaling").forEach { table ->
            tx.run(
                queryOf(
                    "DELETE FROM $table WHERE fnr = :fnr",
                    mapOf("fnr" to fødselsnummer),
                ).asUpdate,
            )
        }
        tx.run(
            queryOf(
                "DELETE FROM person WHERE fnr = :fnr",
                mapOf("fnr" to fødselsnummer.toLong()),
            ).asUpdate,
        )
    }

    private fun lagPersonSlettet(fødselsnummer: String): String =
        """
        {
          "@event_name": "person_slettet",
          "fødselsnummer": "$fødselsnummer"
        }
        """.trimIndent()
}
