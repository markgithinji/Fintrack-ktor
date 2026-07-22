package com.fintrack.core.data

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
            // 1. Try to get URL from Env first (Railway style)
            val envUrl = System.getenv("DATABASE_URL") ?: System.getenv("DB_URL")
            val rawUrl = envUrl ?: config.propertyOrNull("database.url")?.getString() ?: "jdbc:postgresql://localhost:5432/fintrack_db"
            
            val finalUrl = when {
                rawUrl.startsWith("postgres://") -> rawUrl.replace("postgres://", "jdbc:postgresql://")
                rawUrl.startsWith("postgresql://") -> rawUrl.replace("postgresql://", "jdbc:postgresql://")
                else -> rawUrl
            }

            return DatabaseConfig(
                url = finalUrl,
                driver = config.propertyOrNull("database.driver")?.getString() ?: "org.postgresql.Driver",
                user = System.getenv("PGUSER") ?: System.getenv("DB_USER") 
                    ?: config.propertyOrNull("database.user")?.getString() ?: "fintrack",
                password = System.getenv("PGPASSWORD") ?: System.getenv("DB_PASSWORD") 
                    ?: config.propertyOrNull("database.password")?.getString() ?: "secret",
                poolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() 
                    ?: config.propertyOrNull("database.poolSize")?.getString()?.toIntOrNull() ?: 5,
                connectionTimeout = config.propertyOrNull("database.connectionTimeout")?.getString()?.toLongOrNull()
                    ?: TimeUnit.SECONDS.toMillis(30),
                validationTimeout = config.propertyOrNull("database.validationTimeout")?.getString()?.toLongOrNull()
                    ?: TimeUnit.SECONDS.toMillis(5),
                leakDetectionThreshold = config.propertyOrNull("database.leakDetectionThreshold")?.getString()?.toLongOrNull()
                    ?: TimeUnit.SECONDS.toMillis(60),
                minimumIdle = config.propertyOrNull("database.minimumIdle")?.getString()?.toIntOrNull(),
                maxLifetime = config.propertyOrNull("database.maxLifetime")?.getString()?.toLongOrNull()
                    ?: TimeUnit.MINUTES.toMillis(30)
            )
        }
    }
}
