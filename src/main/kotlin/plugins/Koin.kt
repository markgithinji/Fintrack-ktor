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
        // Load properties from Ktor application.yaml
        val redisHost = environment.config.propertyOrNull("redis.host")?.getString()
        val redisPort = environment.config.propertyOrNull("redis.port")?.getString()
        val redisPassword = environment.config.propertyOrNull("redis.password")?.getString()

        properties(
            mapOf(
                "redis.host" to (redisHost ?: "localhost"),
                "redis.port" to (redisPort ?: "6379"),
                "redis.password" to (redisPassword ?: "")
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
