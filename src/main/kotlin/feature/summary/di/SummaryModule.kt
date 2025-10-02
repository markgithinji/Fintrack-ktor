package feature.summary.di

import com.fintrack.feature.summary.data.repository.StatisticsRepository
import feature.summary.domain.StatisticsService
import org.koin.dsl.module

val summaryModule = module {
    single<StatisticsRepository> { StatisticsRepository() }
    single<StatisticsService> { StatisticsService(get()) }
}