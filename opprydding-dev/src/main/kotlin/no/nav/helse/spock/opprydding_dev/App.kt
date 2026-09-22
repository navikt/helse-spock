package no.nav.helse.spock.opprydding_dev

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv()
    RapidApplication
        .create(env)
        .also { rapidApplication ->
            SlettPersonRiver(
                rapidsConnection = rapidApplication,
                dataSource = DataSourceBuilder(env).getDataSource(),
            )
        }.start()
}
