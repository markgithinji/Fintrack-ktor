package com.fintrack.plugins

import com.fintrack.feature.accounts.di.accountsModule
import feature.auth.di.authModule
import feature.budget.di.budgetModule
import feature.summary.di.summaryModule
import feature.transactions.di.transactionsModule
import feature.user.di.userModule
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
            authModule
        )
    }
}
