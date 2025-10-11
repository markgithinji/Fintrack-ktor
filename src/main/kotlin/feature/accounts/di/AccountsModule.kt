package com.fintrack.feature.accounts.di

import com.fintrack.feature.accounts.domain.AccountService
import com.fintrack.feature.accounts.domain.AccountServiceImpl
import com.fintrack.feature.accounts.domain.AccountsRepository
import feature.accounts.data.AccountsRepositoryImpl
import org.koin.dsl.module

val accountsModule = module {
    single<AccountsRepository> { AccountsRepositoryImpl() }
    single<AccountService> { AccountServiceImpl(get()) }
}
