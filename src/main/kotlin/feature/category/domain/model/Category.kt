package feature.category.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

object CategoryConstants {
    val TRANSACTION_FEES_ID: UUID = UUID.fromString("00000000-0000-4000-a000-000000000028")
    val MISC_EXPENSE_ID: UUID = UUID.fromString("99999999-9999-4999-a999-999999999999")
    val OTHER_INCOME_ID: UUID = UUID.fromString("bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb")
}

data class Category(
    val id: UUID,
    val userId: UUID?,
    val name: String,
    val isExpense: Boolean,
    val iconName: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Instant? = null
)
