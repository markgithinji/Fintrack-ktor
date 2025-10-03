package com.fintrack

import core.DatabaseFactory
import com.fintrack.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import plugins.configureStatusPages
import plugins.configureValidation

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureDI()
    configureValidation()
    configureAuth()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}
