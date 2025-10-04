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
    private var dataSource: HikariDataSource? = null
    private val log = logger<DatabaseFactory>()

    @Volatile
    private var initialized = false
    private val initLock = Any()

    fun init(databaseConfig: DatabaseConfig) {
        synchronized(initLock) {
            if (initialized) {
                log.warn("DatabaseFactory already initialized")
                return
            }

            val config = HikariConfig().apply {
                driverClassName = databaseConfig.driver
                jdbcUrl = databaseConfig.url
                username = databaseConfig.user
                password = databaseConfig.password
                maximumPoolSize = databaseConfig.poolSize
                minimumIdle = databaseConfig.minimumIdle ?: (databaseConfig.poolSize / 2)
                maxLifetime = databaseConfig.maxLifetime ?: 1800000
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_READ_COMMITTED"

                connectionTimeout = databaseConfig.connectionTimeout
                validationTimeout = databaseConfig.validationTimeout
                leakDetectionThreshold = databaseConfig.leakDetectionThreshold
                connectionTestQuery = "SELECT 1"

                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            }

            dataSource = HikariDataSource(config)
            Database.connect(dataSource!!)

            log.withContext(
                "url" to config.jdbcUrl.replace(Regex(":[^:]*@"), ":****@"),
                "poolSize" to config.maximumPoolSize,
                "minIdle" to config.minimumIdle
            ).info{ "Database connection pool initialized" }

            // Test connection - let exceptions propagate for startup failure
            testConnection()

            // Run schema migrations - let exceptions propagate
            runMigrations()

            initialized = true
            log.info("DatabaseFactory initialized successfully")
        }
    }

    private fun testConnection() {
        transaction {
            exec("SELECT 1") { }
        }
        log.debug("Initial database connection test passed")
    }

    private fun runMigrations() {
        transaction {
            SchemaUtils.create(TransactionsTable)
        }
        log.info("Database schema initialized successfully")
    }

    fun checkConnection(): Boolean {
        if (!initialized) {
            log.warn("DatabaseFactory not initialized")
            return false
        }

        return try {
            transaction {
                exec("SELECT 1") { }
            }
            log.debug("Database health check passed")
            true
        } catch (e: Exception) {
            log.error("Database health check failed", e)
            false
        }
    }

    fun getPoolStats(): Map<String, Any> {
        val ds = dataSource ?: return emptyMap()

        return try {
            if (!ds.isRunning) {
                return emptyMap()
            }

            val pool = ds.hikariPoolMXBean
            mapOf(
                "activeConnections" to pool.activeConnections,
                "idleConnections" to pool.idleConnections,
                "totalConnections" to pool.totalConnections,
                "threadsAwaitingConnection" to pool.threadsAwaitingConnection,
                "maximumPoolSize" to ds.maximumPoolSize,
                "minimumIdle" to ds.minimumIdle,
                "state" to if (ds.isRunning) "running" else "stopped"
            )
        } catch (e: Exception) {
            log.warn("Failed to get pool stats", e)
            emptyMap()
        }
    }
}