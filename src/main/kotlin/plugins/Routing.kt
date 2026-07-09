package com.fintrack.plugins

import com.fintrack.feature.health.HealthService
import com.fintrack.feature.health.healthRoutes
import com.fintrack.feature.accounts.domain.AccountService
import com.fintrack.feature.auth.authRoutes
import com.fintrack.feature.summary.summaryRoutes
import feature.budget.domain.BudgetService
import feature.summary.domain.StatisticsService
import com.fintrack.feature.accounts.accountsRoutes
import com.fintrack.feature.auth.domain.AuthService
import com.fintrack.feature.user.domain.UserService
import com.fintrack.feature.user.userRoutes
import feature.transaction.budgetRoutes
import feature.transaction.categoryRoutes
import feature.transaction.domain.CategoryService
import feature.transaction.domain.TransactionService
import feature.transaction.transactionRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val accountService: AccountService by inject()
    val transactionService: TransactionService by inject()
    val categoryService: CategoryService by inject()
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
        authenticationRoutes(authService, userService)

        // Business API endpoints
        apiRoutes(accountService, transactionService, categoryService, statisticsService, budgetService, userService)
    }
}

fun Routing.monitoringRoutes(healthService: HealthService) {
    withHealthRateLimit {
        get("/metrics") { call.respondText(appMicrometerRegistry.scrape()) }
        healthRoutes(healthService)
    }
}

fun Routing.authenticationRoutes(authService: AuthService, userService: UserService) {
    authRoutes(authService, userService)
}

fun Routing.apiRoutes(
    accountService: AccountService,
    transactionService: TransactionService,
    categoryService: CategoryService,
    statisticsService: StatisticsService,
    budgetService: BudgetService,
    userService: UserService
) {
    authenticate("auth-jwt") {
        withProtectedRateLimit {
            transactionRoutes(transactionService)
            categoryRoutes(categoryService)
            accountsRoutes(accountService)
            budgetRoutes(budgetService)
            summaryRoutes(statisticsService)
            userRoutes(userService)
        }
    }
}