package feature.summary.di

import com.fintrack.feature.summary.data.repository.StatisticsRepositoryImpl
import feature.summary.domain.StatisticsRepository
import feature.summary.domain.StatisticsService
import feature.summary.domain.StatisticsServiceImpl
import com.fintrack.feature.user.domain.UserRepository
import feature.budget.domain.BudgetRepository
import com.fintrack.feature.accounts.domain.AccountService
import org.koin.dsl.module

val summaryModule = module {
    single<StatisticsRepository> { StatisticsRepositoryImpl() }
    single<StatisticsService> { 
        StatisticsServiceImpl(
            statisticsRepository = get<StatisticsRepository>(),
            userRepository = get<UserRepository>(),
            budgetRepository = get<BudgetRepository>(),
            accountService = get<AccountService>()
        ) 
    }
}
