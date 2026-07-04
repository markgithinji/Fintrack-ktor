package feature.transaction.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class RecurringBillDto(
    val id: String,
    val name: String,
    val amount: Double,
    val category: String,
    val frequency: String,
    val nextDueDate: LocalDate,
    val isActive: Boolean = true
)
