package feature.transactions.di

import feature.transactions.data.TransactionRepository
import feature.transactions.domain.model.TransactionService
import org.koin.dsl.module

val transactionsModule = module {
    single { TransactionRepository() }
    single { TransactionService(get()) }
}
