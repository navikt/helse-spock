package no.nav.helse.spock

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
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
        Tilstandsendringer(this, dataSource)
        IkkePåminnelser(this, dataSource)
        Påminnelser(this, dataSource)
        UtbetalingEndret(this, dataSource)
        UtbetalingPåminnelser(this, dataSource)
        PersonAvstemminger(this, dataSource)
        PersonPåminnelser(this, dataSource)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}
