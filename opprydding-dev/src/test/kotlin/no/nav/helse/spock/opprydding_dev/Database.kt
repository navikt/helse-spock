package no.nav.helse.spock.opprydding_dev

import no.nav.sykepenger.libs.postgres.TestDatabase
import javax.sql.DataSource

internal object Database {
    private val testDatabase = TestDatabase.start().also { it.migrer() }

    val dataSource: DataSource = testDatabase.dataSource

    fun tøm() = testDatabase.tøm("paminnelse", "person", "utbetaling")
}
