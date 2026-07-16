package feature.category.domain

import feature.category.domain.model.CategoryRule

interface CategoryRuleRepository {
    suspend fun getAll(): List<CategoryRule>
}
