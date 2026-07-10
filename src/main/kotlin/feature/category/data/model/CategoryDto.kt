package feature.category.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val isExpense: Boolean,
    val iconName: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Instant? = null
)
