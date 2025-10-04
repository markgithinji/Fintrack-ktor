package com.fintrack.core

import io.ktor.server.config.*
import java.util.concurrent.TimeUnit

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val user: String,
    val password: String,
    val poolSize: Int,
    val connectionTimeout: Long,
    val validationTimeout: Long,
    val leakDetectionThreshold: Long,
    val minimumIdle: Int? = null,
    val maxLifetime: Long? = null
) {
    companion object {
        fun fromEnvironment(config: ApplicationConfig): DatabaseConfig {
            return DatabaseConfig(
                url = config.propertyOrNull("database.url")?.getString()
                    ?: System.getenv("DB_URL")
                    ?: "jdbc:postgresql://localhost:5432/fintrack_db",
                driver = config.propertyOrNull("database.driver")?.getString()
                    ?: "org.postgresql.Driver",
                user = config.propertyOrNull("database.user")?.getString()
                    ?: System.getenv("DB_USER")
                    ?: "fintrack",
                password = config.propertyOrNull("database.password")?.getString()
                    ?: System.getenv("DB_PASSWORD")
                    ?: "secret",
                poolSize = config.propertyOrNull("database.poolSize")?.getString()?.toIntOrNull() ?: 5,
                connectionTimeout = config.propertyOrNull("database.connectionTimeout")?.getString()?.toLongOrNull()
                    ?: TimeUnit.SECONDS.toMillis(30),
                validationTimeout = config.propertyOrNull("database.validationTimeout")?.getString()?.toLongOrNull()
                    ?: TimeUnit.SECONDS.toMillis(5),
                leakDetectionThreshold = config.propertyOrNull("database.leakDetectionThreshold")?.getString()?.toLongOrNull()
                    ?: TimeUnit.SECONDS.toMillis(60),
                minimumIdle = config.propertyOrNull("database.minimumIdle")?.getString()?.toIntOrNull(),
                maxLifetime = config.propertyOrNull("database.maxLifetime")?.getString()?.toLongOrNull()
                    ?: TimeUnit.MINUTES.toMillis(30) // Default 30 minutes
            )
        }
    }
}