package feature.transactions

import core.Category
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// --- Domain ---
data class Budget(
    val id: Int? = null,
    val name: String,
    val categories: List<String>,
    val limit: Double,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)

// --- DTO ---
@Serializable
data class BudgetDto(
    val id: Int? = null,
    val name: String,
    val categories: List<String>,
    val limit: Double,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)

// --- Mappers ---
fun BudgetDto.toDomain(): Budget =
    Budget(
        id = id,
        name = name,
        categories = categories,
        limit = limit,
        isExpense = isExpense,
        startDate = startDate,
        endDate = endDate
    )

fun Budget.toDto(): BudgetDto =
    BudgetDto(
        id = id,
        name = name,
        categories = categories,
        limit = limit,
        isExpense = isExpense,
        startDate = startDate,
        endDate = endDate
    )


val budgets = mutableListOf<Budget>()
