package feature.transaction.di

import feature.transaction.data.CategoryRepositoryImpl
import feature.transaction.data.TransactionRepositoryImpl
import feature.transaction.domain.CategoryRepository
import feature.transaction.domain.CategoryService
import feature.transaction.domain.CategoryServiceImpl
import feature.transaction.domain.TransactionRepository
import feature.transaction.domain.TransactionService
import feature.transaction.domain.TransactionServiceImpl
import org.koin.dsl.module

fun transactionsModule() = module {
    single<TransactionRepository> { TransactionRepositoryImpl() }
    single<TransactionService> { TransactionServiceImpl(get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<CategoryService> { CategoryServiceImpl(get()) }
}