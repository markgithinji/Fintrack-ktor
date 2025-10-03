package com.fintrack.feature.health

import org.koin.dsl.module

fun healthModule() = module {
    single<DatabaseHealthIndicator> { DatabaseHealthIndicator() }
    single<MemoryHealthIndicator> { MemoryHealthIndicator() }
    single<HealthService> { HealthService(get(), get()) }
}