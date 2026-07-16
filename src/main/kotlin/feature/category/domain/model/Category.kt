package feature.category.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class Category(
    val id: UUID,
    val userId: UUID?,
    val name: String,
    val isExpense: Boolean,
    val iconName: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Instant? = null
)
