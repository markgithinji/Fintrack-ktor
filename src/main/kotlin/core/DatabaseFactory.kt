package core

import com.fintrack.core.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import feature.transaction.data.TransactionsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.fintrack.core.logger
import com.fintrack.core.withContext
import java.util.concurrent.TimeUnit

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource
    private val log = logger<DatabaseFactory>()

    fun init(databaseConfig: DatabaseConfig) {
        val config = HikariConfig().apply {
            driverClassName = databaseConfig.driver
            jdbcUrl = databaseConfig.url
            username = databaseConfig.user
            password = databaseConfig.password
            maximumPoolSize = databaseConfig.poolSize
            isAutoCommit = false

            connectionTimeout = databaseConfig.connectionTimeout
            validationTimeout = databaseConfig.validationTimeout
            leakDetectionThreshold = databaseConfig.leakDetectionThreshold
            connectionTestQuery = "SELECT 1"

            validate()
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        log.withContext(
            "url" to config.jdbcUrl.replace(Regex(":[^:]*@"), ":****@"),
            "poolSize" to config.maximumPoolSize
        ).info("Database connection pool initialized")

        // Run schema migrations
        transaction {
            SchemaUtils.create(TransactionsTable)
        }
        log.info("Database schema initialized successfully")
    }

    fun checkConnection(): Boolean {
        transaction {
            // Simple query to test connection
            exec("SELECT 1") { }
        }
        log.debug("Database health check passed")
        return true
    }

    fun getPoolStats(): Map<String, Any> {
        if (!::dataSource.isInitialized || !dataSource.isRunning) {
            return emptyMap()
        }

        val pool = dataSource.hikariPoolMXBean
        return mapOf(
            "activeConnections" to pool.activeConnections,
            "idleConnections" to pool.idleConnections,
            "totalConnections" to pool.totalConnections,
            "threadsAwaitingConnection" to pool.threadsAwaitingConnection,
            "maximumPoolSize" to dataSource.maximumPoolSize
        )
    }
}