package feature.category.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CategoryRule(
    val id: String,
    val keyword: String,
    val categoryId: String,
    val isExpense: Boolean
)
