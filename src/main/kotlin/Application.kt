package com.fintrack

import com.fintrack.core.DatabaseFactory
import com.fintrack.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}
