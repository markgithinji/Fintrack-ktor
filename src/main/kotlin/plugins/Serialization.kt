package com.fintrack.plugins

import com.fintrack.core.appSerializersModule
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
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