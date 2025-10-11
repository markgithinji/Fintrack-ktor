package com.fintrack.plugins

import com.fintrack.core.serialization.appSerializersModule
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = appSerializersModule
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}