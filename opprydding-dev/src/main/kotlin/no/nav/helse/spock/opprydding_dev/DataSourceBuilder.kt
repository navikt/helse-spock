package no.nav.helse.spock.opprydding_dev

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration

internal class DataSourceBuilder(env: Map<String, String>) {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_SPOCK_OPPRYDDING_DEV_JDBC_URL"] ?: String.format(
            "jdbc:postgresql://%s:%s/%s",
            requireNotNull(env["DATABASE_SPOCK_OPPRYDDING_DEV_HOST"]) { "database host must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_SPOCK_OPPRYDDING_DEV_PORT"]) { "database port must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_SPOCK_OPPRYDDING_DEV_DATABASE"]) { "database name must be set if jdbc url is not provided" },
        )
        username = requireNotNull(env["DATABASE_SPOCK_OPPRYDDING_DEV_USERNAME"]) { "databasebrukernavn må settes" }
        password = requireNotNull(env["DATABASE_SPOCK_OPPRYDDING_DEV_PASSWORD"]) { "databasepassord må settes" }
        maximumPoolSize = 3
        connectionTimeout = Duration.ofSeconds(30).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
    }

    internal fun getDataSource() = dataSource
    private val dataSource by lazy { HikariDataSource(hikariConfig) }
}
