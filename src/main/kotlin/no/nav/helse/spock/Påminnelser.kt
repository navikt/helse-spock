package no.nav.helse.spock

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class Påminnelser(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Påminnelser::class.java)
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection)
            .validate {
                it.demandAny("@event_name", listOf("minutt", "kjør_spock"))
            }
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        lagPåminnelser(context)
    }

    private fun lagPåminnelser(context: MessageContext) {
        hentPåminnelser(dataSource) { påminnelser ->
            log.info("hentet ${påminnelser.size} påminnelser fra db")
            secureLogger.info("hentet ${påminnelser.size} påminnelser fra db")
            påminnelser.map {
                it.fødselsnummer to it.toJson()
            }.onEach { (_, påminnelse) ->
                secureLogger.info("Produserer $påminnelse")
            }.forEach { (key, value) ->
                context.publish(key, value)
            }
        }
    }
}
