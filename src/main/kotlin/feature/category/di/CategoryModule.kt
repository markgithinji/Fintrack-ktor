package feature.category.di

import feature.category.data.repository.CategoryRepositoryImpl
import feature.category.data.repository.CategoryRuleRepositoryImpl
import feature.category.domain.*
import org.koin.dsl.module

val categoryModule = module {
    single { CategoryMatcher() }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<CategoryService> { CategoryServiceImpl(get()) }
    single<CategoryRuleRepository> { CategoryRuleRepositoryImpl() }
    single<CategoryRuleService> { CategoryRuleServiceImpl(get()) }
}
