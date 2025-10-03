package com.fintrack

import com.fintrack.core.DatabaseConfig
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.plugins.*
import core.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.netty.*
import plugins.configureStatusPages
import plugins.configureValidation
import io.ktor.server.engine.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val databaseConfig = DatabaseConfig.fromEnvironment(environment.config)
    JwtConfig.init(environment.config)

    DatabaseFactory.init(databaseConfig)
    configureLogging()
    configureDI()
    configureValidation()
    configureAuth()
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureMetrics()
    configureHealthChecks()
    configureRouting()
}