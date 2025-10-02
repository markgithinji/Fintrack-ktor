package feature.summary.di

import com.fintrack.feature.summary.data.repository.StatisticsRepositoryImpl
import feature.summary.domain.StatisticsRepository
import feature.summary.domain.StatisticsService
import feature.summary.domain.StatisticsServiceImpl
import org.koin.dsl.module

val summaryModule = module {
    single<StatisticsRepository> { StatisticsRepositoryImpl() }
    single<StatisticsService> { StatisticsServiceImpl(get()) }
}