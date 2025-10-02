package feature.budget.di

import com.fintrack.feature.budget.data.BudgetRepository
import feature.budget.domain.BudgetService
import feature.budget.domain.BudgetServiceImpl
import org.koin.dsl.module

val budgetModule = module {
    single<BudgetRepository> { BudgetRepository() }
    single<BudgetService> { BudgetServiceImpl(get()) }
}