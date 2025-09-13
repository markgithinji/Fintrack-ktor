package feature.transactions

import core.CategorySummariesDto
import core.CategorySummaryDto
import core.HighlightDto
import core.HighlightsDto
import core.SummaryDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Summary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val incomeHighlights: Highlights = Highlights(),
    val expenseHighlights: Highlights = Highlights(),
    val incomeCategorySummary: CategorySummaries = CategorySummaries(),
    val expenseCategorySummary: CategorySummaries = CategorySummaries()
)

@Serializable
data class Highlights(
    val highestMonth: HighestMonth? = null,
    val highestCategory: HighestCategory? = null,
    val highestDay: HighestDay? = null,
    val averagePerDay: Double = 0.0
)

@Serializable
data class CategorySummaries(
    val weekly: Map<String, List<CategorySummary>> = emptyMap(),
    val monthly: Map<String, List<CategorySummary>> = emptyMap()
)

@Serializable
data class HighestMonth(val month: String, val amount: Double)

@Serializable
data class HighestCategory(val category: String, val amount: Double)

@Serializable
data class HighestDay(val date: LocalDate, val amount: Double)

@Serializable
data class CategorySummary(
    val category: String,
    val total: Double,
    val percentage: Double
)

// --------- Mappers ---------
fun HighestMonth.toDto(): HighlightDto =
    HighlightDto(label = month, value = month, amount = amount)

fun HighestCategory.toDto(): HighlightDto =
    HighlightDto(label = category, value = category, amount = amount)

fun HighestDay.toDto(): HighlightDto =
    HighlightDto(label = date.toString(), value = date.toString(), amount = amount)

fun CategorySummary.toDto(): CategorySummaryDto =
    CategorySummaryDto(category = category, total = total, percentage = percentage)

fun Highlights.toDto(): HighlightsDto = HighlightsDto(
    highestMonth = highestMonth?.toDto(),
    highestCategory = highestCategory?.toDto(),
    highestDay = highestDay?.toDto(),
    averagePerDay = averagePerDay
)

fun CategorySummaries.toDto(): CategorySummariesDto = CategorySummariesDto(
    weekly = weekly.mapValues { entry -> entry.value.map { it.toDto() } },
    monthly = monthly.mapValues { entry -> entry.value.map { it.toDto() } }
)

fun Summary.toDto(): SummaryDto = SummaryDto(
    income = income,
    expense = expense,
    balance = balance,
    incomeHighlights = incomeHighlights.toDto(),
    expenseHighlights = expenseHighlights.toDto(),
    incomeCategorySummary = incomeCategorySummary.toDto(),
    expenseCategorySummary = expenseCategorySummary.toDto()
)