package core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import feature.transactions.TransactionsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/fintrack_db"
            username = System.getenv("DB_USER") ?: "fintrack"
            password = System.getenv("DB_PASSWORD") ?: "secret"
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Run schema migrations (auto create tables for now)
        transaction {
            SchemaUtils.create(TransactionsTable)
        }
    }
}
