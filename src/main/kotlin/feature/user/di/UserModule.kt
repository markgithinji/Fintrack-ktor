package com.fintrack.feature.user.di

import com.fintrack.feature.user.data.UserRepositoryImpl
import com.fintrack.feature.user.domain.UserRepository
import com.fintrack.feature.user.domain.UserService
import com.fintrack.feature.user.domain.UserServiceImpl
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.core.EmailService
import com.fintrack.feature.auth.domain.repository.EmailVerificationRepository
import org.koin.dsl.module

fun userModule() = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<UserService> { 
        UserServiceImpl(
            get<UserRepository>(), 
            get<AccountsRepository>(), 
            get<EmailService>(), 
            get<EmailVerificationRepository>()
        ) 
    }
}
