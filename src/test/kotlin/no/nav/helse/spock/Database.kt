package no.nav.helse.spock

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers

val databaseContainer = DatabaseContainers.container("spock", CleanupStrategy.tables("paminnelse, person, utbetaling"))