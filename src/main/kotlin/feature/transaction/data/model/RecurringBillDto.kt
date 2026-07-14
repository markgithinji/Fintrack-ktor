package feature.transaction.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class RecurringBillDto(
    val id: String,
    val name: String,
    @Contextual val amount: BigDecimal,
    val category: String,
    val categoryId: String?,
    val frequency: String,
    val nextDueDate: LocalDate,
    val isActive: Boolean = true
)
