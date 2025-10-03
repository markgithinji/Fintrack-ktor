package core

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

    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/fintrack_db"
            username = System.getenv("DB_USER") ?: "fintrack"
            password = System.getenv("DB_PASSWORD") ?: "secret"
            maximumPoolSize = 5
            isAutoCommit = false

            // Add health check related settings
            connectionTimeout = TimeUnit.SECONDS.toMillis(30) // 30 seconds
            validationTimeout = TimeUnit.SECONDS.toMillis(5)  // 5 seconds
            leakDetectionThreshold = TimeUnit.SECONDS.toMillis(60) // 1 minute
            connectionTestQuery = "SELECT 1" // Simple query to test connections

            validate()
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        log.withContext(
            "url" to config.jdbcUrl.replace(Regex(":[^:]*@"), ":****@"), // Hide password in logs
            "poolSize" to config.maximumPoolSize
        ).info("Database connection pool initialized")

        // Run schema migrations (auto create tables for now)
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