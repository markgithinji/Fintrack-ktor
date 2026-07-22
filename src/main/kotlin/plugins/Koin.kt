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

fun Application.configureDI() {
    install(Koin) {
        // Resolve properties manually from Env or Config to avoid YAML parser bugs
        val redisHost = System.getenv("REDISHOST") ?: System.getenv("REDIS_HOST") 
            ?: environment.config.propertyOrNull("redis.host")?.getString() ?: "localhost"
            
        val redisPort = System.getenv("REDISPORT") ?: System.getenv("REDIS_PORT") 
            ?: environment.config.propertyOrNull("redis.port")?.getString() ?: "6379"
            
        val redisPassword = System.getenv("REDISPASSWORD") ?: System.getenv("REDIS_PASSWORD") 
            ?: environment.config.propertyOrNull("redis.password")?.getString() ?: ""

        properties(
            mapOf(
                "redis.host" to redisHost,
                "redis.port" to redisPort,
                "redis.password" to redisPassword
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
