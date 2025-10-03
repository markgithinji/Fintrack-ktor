package com.fintrack.plugins

import com.fintrack.feature.health.HealthService
import com.fintrack.feature.health.healthRoutes
import com.fintrack.feature.accounts.domain.AccountService
import com.fintrack.feature.auth.authRoutes
import com.fintrack.feature.summary.summaryRoutes
import feature.auth.domain.AuthService
import feature.budget.domain.BudgetService
import feature.summary.domain.StatisticsService
import feature.transaction.accountsRoutes
import feature.transaction.budgetRoutes
import feature.transaction.domain.TransactionService
import feature.transaction.transactionRoutes
import feature.user.domain.UserService
import feature.user.userRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val accountService: AccountService by inject()
    val transactionService: TransactionService by inject()
    val statisticsService: StatisticsService by inject()
    val budgetService: BudgetService by inject()
    val userService: UserService by inject()
    val authService: AuthService by inject()
    val healthService: HealthService by inject()

    routing {
        // Core endpoints
        get("/") { call.respondText("Hello World!") }

        // Monitoring endpoints
        monitoringRoutes(healthService)

        // Authentication endpoints
        authenticationRoutes(authService)

        // Business API endpoints
        apiRoutes(accountService, transactionService, statisticsService, budgetService, userService)
    }
}

fun Routing.monitoringRoutes(healthService: HealthService) {
    withHealthRateLimit {
        get("/metrics") { call.respondText(appMicrometerRegistry.scrape()) }
        healthRoutes(healthService)
    }
}

fun Routing.authenticationRoutes(authService: AuthService) {
    withAuthRateLimit {
        authRoutes(authService)
    }
}

fun Routing.apiRoutes(
    accountService: AccountService,
    transactionService: TransactionService,
    statisticsService: StatisticsService,
    budgetService: BudgetService,
    userService: UserService
) {
    authenticate("auth-jwt") {
        withProtectedRateLimit {
            // Financial data
            route("/transactions") { transactionRoutes(transactionService) }
            route("/accounts") { accountsRoutes(accountService) }
            route("/budgets") { budgetRoutes(budgetService) }

            // Analytics
            route("/summary") { summaryRoutes(statisticsService) }

            // User management
            route("/user") { userRoutes(userService) }
        }
    }
}