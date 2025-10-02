package feature.user.di

import com.fintrack.feature.user.data.UserRepository
import feature.user.domain.UserService
import feature.user.domain.UserServiceImpl
import org.koin.dsl.module

val userModule = module {
    single<UserRepository> { UserRepository() }
    single<UserService> { UserServiceImpl(get()) }
}