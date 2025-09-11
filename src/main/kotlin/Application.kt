package com.fintrack

import core.DatabaseFactory
import com.fintrack.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import plugins.configureStatusPages

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}
