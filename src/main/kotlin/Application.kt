package com.fintrack

import com.fintrack.feature.user.UsersTable
import core.DatabaseFactory
import com.fintrack.plugins.*
import feature.transactions.BudgetsTable
import feature.transactions.data.TransactionsTable
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import plugins.configureStatusPages

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureAuth()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}
