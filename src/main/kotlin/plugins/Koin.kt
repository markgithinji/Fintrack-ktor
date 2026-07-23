package com.fintrack.plugins

import com.fintrack.feature.health.healthModule
import com.fintrack.feature.accounts.di.accountsModule
import com.fintrack.feature.auth.di.authModule
import com.fintrack.feature.user.di.userModule
import feature.budget.di.budgetModule
import feature.category.di.categoryModule
import feature.summary.di.summaryModule
import feature.transaction.di.transactionsModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import java.net.URI

fun Application.configureDI() {
    install(Koin) {
        // 1. Try to get the full REDIS_URL first
        val redisUrlStr = System.getenv("REDIS_URL") ?: System.getenv("REDIS_TLS_URL")
        
        var finalHost = "localhost"
        var finalPort = "6379"
        var finalPassword = ""
        var useSsl = false

        if (redisUrlStr != null) {
            try {
                val uri = URI(redisUrlStr)
                finalHost = uri.host
                finalPort = uri.port.toString()
                finalPassword = uri.userInfo?.split(":")?.getOrNull(1) ?: ""
                useSsl = uri.scheme == "rediss"
            } catch (e: Exception) {
                // Fallback to separate variables if URI parsing fails
            }
        } else {
            // 2. Fallback to separate environment variables
            finalHost = System.getenv("REDISHOST") ?: System.getenv("REDIS_HOST") 
                ?: environment.config.propertyOrNull("redis.host")?.getString() ?: "localhost"
            
            finalPort = System.getenv("REDISPORT") ?: System.getenv("REDIS_PORT") 
                ?: environment.config.propertyOrNull("redis.port")?.getString() ?: "6379"
            
            finalPassword = System.getenv("REDISPASSWORD") ?: System.getenv("REDIS_PASSWORD") 
                ?: environment.config.propertyOrNull("redis.password")?.getString() ?: ""
                
            useSsl = System.getenv("REDIS_SSL")?.toBoolean() ?: false
        }

        properties(
            mapOf(
                "redis.host" to finalHost,
                "redis.port" to finalPort,
                "redis.password" to finalPassword,
                "redis.ssl" to useSsl.toString()
            )
        )

        modules(
            accountsModule,
            transactionsModule(),
            categoryModule,
            summaryModule,
            budgetModule,
            userModule(),
            authModule,
            healthModule()
        )
    }
}
