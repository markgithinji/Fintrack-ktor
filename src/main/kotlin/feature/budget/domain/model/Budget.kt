package feature.transaction

import kotlinx.datetime.LocalDate
import java.util.*

data class Budget(
    val id: UUID? = null,
    val accountId: UUID,
    val name: String,
    val categories: List<String>,
    val limit: Double,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)