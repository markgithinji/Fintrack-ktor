package feature.category.data.repository

import com.fintrack.core.data.dbQuery
import feature.category.data.model.toCategoryRule
import feature.category.data.table.CategoryRulesTable
import feature.category.domain.CategoryRuleRepository
import feature.category.domain.model.CategoryRule
import org.jetbrains.exposed.sql.selectAll

class CategoryRuleRepositoryImpl : CategoryRuleRepository {
    override suspend fun getAll(): List<CategoryRule> = dbQuery {
        CategoryRulesTable.selectAll()
            .map { it.toCategoryRule() }
    }
}
