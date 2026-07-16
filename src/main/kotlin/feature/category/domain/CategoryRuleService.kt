package feature.category.domain

import com.fintrack.core.domain.Result
import feature.category.domain.model.CategoryRule

interface CategoryRuleService {
    suspend fun getAllRules(): Result<List<CategoryRule>>
}
