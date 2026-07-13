package com.fintrack

import com.fintrack.core.data.DatabaseConfig
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.plugins.*
import com.fintrack.core.data.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.doublereceive.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val databaseConfig = DatabaseConfig.fromEnvironment(environment.config)
    JwtConfig.init(environment.config)

    DatabaseFactory.init(databaseConfig)
    install(DoubleReceive)
    configureSecurity()
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
