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

    val highestMonth: HighestMonth?,
    val highestCategory: HighestCategory?,
    val highestDay: HighestDay?,

    val highestIncomeMonth: HighestMonth? = null,
    val highestIncomeCategory: HighestCategory? = null,
    val highestIncomeDay: HighestDay? = null,

    val averagePerDay: Double,

    val weeklyCategorySummary: Map<String, List<CategorySummary>>,
    val monthlyCategorySummary: Map<String, List<CategorySummary>>
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
    highestMonth = highestMonth?.toDto(),
    highestCategory = highestCategory?.toDto(),
    highestDay = highestDay?.toDto(),
    highestIncomeMonth = highestIncomeMonth?.toDto(),
    highestIncomeCategory = highestIncomeCategory?.toDto(),
    highestIncomeDay = highestIncomeDay?.toDto(),
    averagePerDay = averagePerDay,
    weeklyCategorySummary = weeklyCategorySummary.mapValues { it.value.map { cs -> cs.toDto() } },
    monthlyCategorySummary = monthlyCategorySummary.mapValues { it.value.map { cs -> cs.toDto() } }
)

