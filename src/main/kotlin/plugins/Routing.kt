package com.fintrack.plugins

import com.fintrack.feature.auth.authRoutes
import feature.transactions.accountsRoutes
import feature.transactions.budgetRoutes
import feature.transactions.transactionRoutes
import feature.user.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        authRoutes()

        authenticate("auth-jwt") {
            transactionRoutes()
            budgetRoutes()
            userRoutes()
            accountsRoutes()
        }
    }
}