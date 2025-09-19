package feature.transactions

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// --- Domain ---
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


// --- DTO ---
@Serializable
data class BudgetDto(
    val id: Int? = null,
    val accountId: Int,
    val name: String,
    val categories: List<String>,
    val limit: Double,
    val isExpense: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate
)


// --- Mappers ---
fun BudgetDto.toDomain(userId: Int): Budget =
    Budget(
        id = id,
        userId = userId,
        accountId = accountId,
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
        accountId = accountId,
        name = name,
        categories = categories,
        limit = limit,
        isExpense = isExpense,
        startDate = startDate,
        endDate = endDate
    )
