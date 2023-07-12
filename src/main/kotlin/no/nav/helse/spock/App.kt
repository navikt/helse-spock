package no.nav.helse.spock

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.helse.Spock")

fun main() {
    val env = System.getenv().toMutableMap()
    Thread.currentThread().setUncaughtExceptionHandler { t, e ->
        log.error("{}", e.message, e)
    }
    launchApp(env)
}

fun launchApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()

    RapidApplication.create(env).apply {
        Forkastelser(this, dataSource)
        BogusPåminnelser(this, dataSource)
        Tilstandsendringer(this, dataSource)
        IkkePåminnelser(this, dataSource)
        Påminnelser(this, dataSource)
        UtbetalingEndret(this, dataSource)
        UtbetalingPåminnelser(this, dataSource)
//        PersonAvstemminger(this, dataSource)
        PersonPåminnelser(this, dataSource)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}
