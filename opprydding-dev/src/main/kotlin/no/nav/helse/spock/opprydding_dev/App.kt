package no.nav.helse.spock.opprydding_dev

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.helse.spock.opprydding_dev")

fun main() {
    val env = System.getenv()
    log.info("Starting opprydding-dev")

    val dataSource = DataSourceBuilder(env).getDataSource()

    RapidApplication
        .create(env)
        .apply {
            SlettPersonRiver(this, dataSource)
        }
        .start()
}
