package com.fintrack.plugins

import com.fintrack.feature.accounts.di.accountsModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.configureDI() {
    install(Koin) {
        modules(
            accountsModule,
        )
    }
}
