package feature.auth.di

import feature.auth.domain.AuthService
import feature.auth.domain.AuthServiceImpl
import org.koin.dsl.module

val authModule = module {
    single<AuthService> { AuthServiceImpl(get()) }
}
