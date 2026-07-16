package feature.category.domain

import com.fintrack.core.domain.Result
import feature.category.domain.model.CategoryRule

class CategoryRuleServiceImpl(
    private val categoryRuleRepository: CategoryRuleRepository
) : CategoryRuleService {
    override suspend fun getAllRules(): Result<List<CategoryRule>> {
        return Result.Success(categoryRuleRepository.getAll())
    }
}
