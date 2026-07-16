package feature.category.data.table

import com.fintrack.core.data.TableNames
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object CategoryRulesTable : UUIDTable(TableNames.CATEGORY_RULES) {
    val keyword = varchar(CategoryRulesColumns.KEYWORD, 255).uniqueIndex()
    val categoryId = reference(CategoryRulesColumns.CATEGORY_ID, CategoriesTable, onDelete = ReferenceOption.CASCADE)
    val isExpense = bool(CategoryRulesColumns.IS_EXPENSE)
}
