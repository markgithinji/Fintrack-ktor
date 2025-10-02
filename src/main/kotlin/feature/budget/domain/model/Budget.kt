package feature.transactions

import kotlinx.datetime.LocalDate

data class Budget(
    val id: Int? = null,
    val userId: Int,
    val accountId: Int,
    val name: String,
    val categories: List<String>,
    val limit: Double,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)