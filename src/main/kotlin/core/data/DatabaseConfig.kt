package com.fintrack.core.data

import io.ktor.server.config.*
import java.net.URI
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

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
        private val log = LoggerFactory.getLogger("DatabaseConfig")

        fun fromEnvironment(config: ApplicationConfig): DatabaseConfig {
            val envUrl = System.getenv("DATABASE_URL") ?: System.getenv("DB_URL") ?: System.getenv("POSTGRES_URL")
            
            var finalUrl = config.propertyOrNull("database.url")?.getString() ?: "jdbc:postgresql://localhost:5432/fintrack_db"
            var finalUser = System.getenv("PGUSER") ?: System.getenv("DB_USER") ?: config.propertyOrNull("database.user")?.getString() ?: "fintrack"
            var finalPassword = System.getenv("PGPASSWORD") ?: System.getenv("DB_PASSWORD") ?: config.propertyOrNull("database.password")?.getString() ?: "secret"

            if (envUrl != null) {
                try {
                    val uri = URI(envUrl)
                    val userInfo = uri.userInfo?.split(":")
                    
                    val host = uri.host
                    val port = if (uri.port != -1) uri.port else 5432
                    val path = uri.path
                    
                    finalUrl = "jdbc:postgresql://$host:$port$path"
                    if (uri.query != null) {
                        finalUrl += "?${uri.query}"
                    }
                    
                    finalUser = userInfo?.getOrNull(0) ?: finalUser
                    finalPassword = userInfo?.getOrNull(1) ?: finalPassword
                    
                    log.info("Parsed DATABASE_URL successfully for host: $host")
                } catch (e: Exception) {
                    log.error("Failed to parse DATABASE_URL: ${e.message}. Falling back to default resolution.")
                    // Fallback to simple replacement if URI parsing fails for some reason
                    finalUrl = envUrl.replace("postgres://", "jdbc:postgresql://")
                        .replace("postgresql://", "jdbc:postgresql://")
                }
            } else {
                // Ensure the URL starts with jdbc:
                if (!finalUrl.startsWith("jdbc:")) {
                    finalUrl = "jdbc:postgresql://" + finalUrl.removePrefix("postgres://").removePrefix("postgresql://")
                }
            }

            return DatabaseConfig(
                url = finalUrl,
                driver = config.propertyOrNull("database.driver")?.getString() ?: "org.postgresql.Driver",
                user = finalUser,
                password = finalPassword,
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
