package feature.transactions.di

import feature.transactions.data.TransactionRepositoryImpl
import feature.transactions.domain.TransactionRepository
import feature.transactions.domain.TransactionService
import feature.transactions.domain.TransactionServiceImpl
import org.koin.dsl.module

fun transactionsModule() = module {
    single<TransactionRepository> { TransactionRepositoryImpl() }
    single<TransactionService> { TransactionServiceImpl(get()) }
}