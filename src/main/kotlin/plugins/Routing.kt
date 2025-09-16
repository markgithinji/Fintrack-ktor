package com.fintrack.plugins

import com.fintrack.feature.user.authRoutes
import feature.transactions.budgetRoutes
import feature.transactions.transactionRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        transactionRoutes()
        budgetRoutes()
        authRoutes()
    }
}
