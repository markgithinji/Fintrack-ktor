package com.fintrack

import com.fintrack.plugins.*
import core.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.netty.*
import plugins.configureStatusPages
import plugins.configureValidation

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
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
