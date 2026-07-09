package com.fintrack.plugins

import com.fintrack.feature.health.healthModule
import com.fintrack.feature.accounts.di.accountsModule
import com.fintrack.feature.auth.di.authModule
import com.fintrack.feature.user.di.userModule
import feature.budget.di.budgetModule
import feature.summary.di.summaryModule
import feature.transaction.di.transactionsModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.configureDI() {
    install(Koin) {
        modules(
            accountsModule,
            transactionsModule(),
            summaryModule,
            budgetModule,
            userModule(),
            authModule,
            healthModule()
        )
    }
}
