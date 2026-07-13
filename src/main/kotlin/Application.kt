package com.fintrack

import com.fintrack.core.data.DatabaseConfig
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.plugins.*
import com.fintrack.core.data.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.doublereceive.*
import org.koin.ktor.ext.inject
import redis.clients.jedis.JedisPool

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
    
    val jedisPool by inject<JedisPool>()

    configureValidation()
    configureAuth()
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureMetrics()
    configureHealthChecks()
    configureRouting()

    monitor.subscribe(ApplicationStopped) {
        DatabaseFactory.close()
        jedisPool.close()
    }
}
