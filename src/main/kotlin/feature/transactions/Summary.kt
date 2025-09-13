package feature.transactions

import core.CategorySummaryDto
import core.DistributionSummaryDto
import core.HighlightDto
import core.HighlightsDto
import core.HighlightsSummaryDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// --------- Domain ---------
@Serializable
data class HighlightsSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val incomeHighlights: Highlights = Highlights(),
    val expenseHighlights: Highlights = Highlights()
)

@Serializable
data class DistributionSummary(
    val period: String = "",
    val incomeCategories: List<CategorySummary> = emptyList(),
    val expenseCategories: List<CategorySummary> = emptyList()
)

@Serializable
data class Highlights(
    val highestMonth: Highlight? = null,
    val highestCategory: Highlight? = null,
    val highestDay: Highlight? = null,
    val averagePerDay: Double = 0.0
)

@Serializable
data class Highlight(
    val label: String = "",
    val value: String = "",
    val amount: Double = 0.0
)

@Serializable
data class CategorySummary(
    val category: String = "",
    val total: Double = 0.0,
    val percentage: Double = 0.0
)

// --------- Mappers to DTO ---------
fun Highlight.toDto(): HighlightDto =
    HighlightDto(label = label, value = value, amount = amount)

fun Highlights.toDto(): HighlightsDto = HighlightsDto(
    highestMonth = highestMonth?.toDto(),
    highestCategory = highestCategory?.toDto(),
    highestDay = highestDay?.toDto(),
    averagePerDay = averagePerDay
)

fun CategorySummary.toDto(): CategorySummaryDto =
    CategorySummaryDto(category = category, total = total, percentage = percentage)

fun HighlightsSummary.toDto(): HighlightsSummaryDto = HighlightsSummaryDto(
    income = income,
    expense = expense,
    balance = balance,
    incomeHighlights = incomeHighlights.toDto(),
    expenseHighlights = expenseHighlights.toDto()
)

fun DistributionSummary.toDto(): DistributionSummaryDto = DistributionSummaryDto(
    period = period,
    incomeCategories = incomeCategories.map { it.toDto() },
    expenseCategories = expenseCategories.map { it.toDto() }
)
