package feature.category.di

import feature.category.data.repository.CategoryRepositoryImpl
import feature.category.domain.CategoryRepository
import feature.category.domain.CategoryService
import feature.category.domain.CategoryServiceImpl
import org.koin.dsl.module

val categoryModule = module {
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<CategoryService> { CategoryServiceImpl(get()) }
}
