package feature.transactions

import core.CategorySummaryDto
import core.HighlightDto
import core.SummaryDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable


@Serializable
data class Summary(
    val income: Double,
    val expense: Double,
    val balance: Double,

    // Expense highlights
    val highestMonth: HighestMonth?,
    val highestCategory: HighestCategory?,
    val highestDay: HighestDay?,
    val averagePerDay: Double,

    // Income highlights
    val highestIncomeMonth: HighestMonth? = null,
    val highestIncomeCategory: HighestCategory? = null,
    val highestIncomeDay: HighestDay? = null,
    val averageIncomePerDay: Double = 0.0,

    // Expense categories
    val weeklyCategorySummary: Map<String, List<CategorySummary>> = emptyMap(),
    val monthlyCategorySummary: Map<String, List<CategorySummary>> = emptyMap(),

    // Income categories
    val weeklyIncomeCategorySummary: Map<String, List<CategorySummary>> = emptyMap(),
    val monthlyIncomeCategorySummary: Map<String, List<CategorySummary>> = emptyMap()
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

fun HighestMonth.toDto(): HighlightDto =
    HighlightDto(label = month, value = month, amount = amount)

fun HighestCategory.toDto(): HighlightDto =
    HighlightDto(label = category, value = category, amount = amount)

fun HighestDay.toDto(): HighlightDto =
    HighlightDto(label = date.toString(), value = date.toString(), amount = amount)

fun CategorySummary.toDto(): CategorySummaryDto =
    CategorySummaryDto(category = category, total = total, percentage = percentage)

fun Summary.toDto(): SummaryDto = SummaryDto(
    income = income,
    expense = expense,
    balance = balance,

    // Expense highlights
    highestMonth = highestMonth?.toDto(),
    highestCategory = highestCategory?.toDto(),
    highestDay = highestDay?.toDto(),
    averagePerDay = averagePerDay,

    // Income highlights
    highestIncomeMonth = highestIncomeMonth?.toDto(),
    highestIncomeCategory = highestIncomeCategory?.toDto(),
    highestIncomeDay = highestIncomeDay?.toDto(),
    averageIncomePerDay = averageIncomePerDay,

    // Expense categories
    weeklyCategorySummary = weeklyCategorySummary.mapValues { it.value.map { cs -> cs.toDto() } },
    monthlyCategorySummary = monthlyCategorySummary.mapValues { it.value.map { cs -> cs.toDto() } },

    // Income categories
    weeklyIncomeCategorySummary = weeklyIncomeCategorySummary.mapValues { it.value.map { cs -> cs.toDto() } },
    monthlyIncomeCategorySummary = monthlyIncomeCategorySummary.mapValues { it.value.map { cs -> cs.toDto() } }
)

