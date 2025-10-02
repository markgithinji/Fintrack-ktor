package com.fintrack.plugins

import com.fintrack.feature.accounts.domain.AccountService
import com.fintrack.feature.auth.authRoutes
import com.fintrack.feature.summary.summaryRoutes
import feature.auth.domain.AuthService
import feature.budget.domain.BudgetService
import feature.summary.domain.StatisticsService
import feature.transactions.accountsRoutes
import feature.transactions.budgetRoutes
import feature.transactions.domain.model.TransactionService
import feature.transactions.transactionRoutes
import feature.user.domain.UserService
import feature.user.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val accountService: AccountService by inject()
    val transactionService: TransactionService by inject()
    val statisticsService: StatisticsService by inject()
    val budgetService: BudgetService by inject()
    val userService: UserService by inject()
    val authService: AuthService by inject()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        authRoutes(authService)

        authenticate("auth-jwt") {
            transactionRoutes(transactionService)
            budgetRoutes(budgetService)
            userRoutes(userService)
            accountsRoutes(accountService)
            summaryRoutes(statisticsService)
        }
    }
}