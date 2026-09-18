package no.nav.helse.spock.opprydding_dev

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers

// Gjenbruker migreringene fra :spock-modulen (samme databaseskjema), siden opprydding-dev
// opererer på den samme databasen som selve spock-appen.
val databaseContainer = DatabaseContainers.container("spock-opprydding-dev", CleanupStrategy.tables("paminnelse, person, utbetaling"))
