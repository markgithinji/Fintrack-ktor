package feature.category.data.model

import feature.category.data.table.CategoryRulesTable
import feature.category.domain.model.CategoryRule
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCategoryRule() = CategoryRule(
    id = this[CategoryRulesTable.id].value.toString(),
    keyword = this[CategoryRulesTable.keyword],
    categoryId = this[CategoryRulesTable.categoryId].toString(),
    isExpense = this[CategoryRulesTable.isExpense]
)
