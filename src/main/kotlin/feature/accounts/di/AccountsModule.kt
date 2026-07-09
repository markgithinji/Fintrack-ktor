package com.fintrack.feature.accounts.di

import com.fintrack.feature.accounts.domain.AccountService
import com.fintrack.feature.accounts.domain.AccountServiceImpl
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.feature.accounts.data.repository.AccountsRepositoryImpl
import org.koin.dsl.module

val accountsModule = module {
    single<AccountsRepository> { AccountsRepositoryImpl() }
    single<AccountService> { AccountServiceImpl(accountsRepository = get()) }
}
