package feature.budget.di

import com.fintrack.feature.budget.data.BudgetRepositoryImpl
import feature.budget.domain.BudgetRepository
import feature.budget.domain.BudgetService
import feature.budget.domain.BudgetServiceImpl
import org.koin.dsl.module

val budgetModule = module {
    single<BudgetRepository> { BudgetRepositoryImpl() }
    single<BudgetService> { BudgetServiceImpl(get()) }
}