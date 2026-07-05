package feature.summary.data

import feature.summary.data.model.AvailableMonthsDto
import feature.summary.data.model.AvailableWeeksDto
import feature.summary.data.model.AvailableYearsDto
import feature.summary.data.model.CategoryComparisonDto
import feature.summary.data.model.CategorySummaryDto
import feature.summary.data.model.DistributionSummaryDto
import feature.summary.data.model.DaySummaryDto
import feature.summary.data.model.HighlightDto
import feature.summary.data.model.HighlightsDto
import feature.summary.data.model.OverviewSummaryDto
import feature.summary.data.model.StatisticsSummaryDto
import feature.summary.domain.model.CategoryComparison
import feature.summary.domain.model.CategorySummary
import feature.summary.domain.model.DaySummary
import feature.summary.domain.model.DistributionSummary
import feature.summary.domain.model.OverviewSummary
import feature.summary.domain.model.AvailableMonths
import feature.summary.domain.model.AvailableWeeks
import feature.summary.domain.model.AvailableYears
import feature.summary.domain.model.Highlight
import feature.summary.domain.model.Highlights
import feature.summary.domain.model.StatisticsSummary

// Highlights summary
fun StatisticsSummary.toDto(): StatisticsSummaryDto = StatisticsSummaryDto(
    period = period,
    isCurrent = isCurrent,
    income = income,
    expense = expense,
    balance = balance,
    totalTransactionCost = totalTransactionCost,
    incomeHighlights = incomeHighlights.toDto(),
    expenseHighlights = expenseHighlights.toDto(),
)

fun Highlights.toDto(): HighlightsDto = HighlightsDto(
    highestMonth = highestMonth?.toDto(),
    highestCategory = highestCategory?.toDto(),
    highestDay = highestDay?.toDto(),
    averagePerDay = averagePerDay
)

fun Highlight.toDto(): HighlightDto =
    HighlightDto(label = label, value = value, amount = amount)


// Distribution summary
fun DistributionSummary.toDto(): DistributionSummaryDto = DistributionSummaryDto(
    period = period,
    incomeCategories = incomeCategories.map { it.toDto() },
    expenseCategories = expenseCategories.map { it.toDto() }
)

fun CategorySummary.toDto(): CategorySummaryDto =
    CategorySummaryDto(category = category, total = total, percentage = percentage)

// Overview summary
fun OverviewSummary.toDto() = OverviewSummaryDto(
    period = period,
    isCurrent = isCurrent,
    weeklyOverview = weeklyOverview.map { it.toDto() },
    monthlyOverview = monthlyOverview.map { it.toDto() }
)

fun DaySummary.toDto() = DaySummaryDto(
    date = date,
    income = income,
    expense = expense
)

// Category comparison
fun CategoryComparison.toDto() = CategoryComparisonDto(
    period = period,
    category = category,
    currentTotal = currentTotal,
    previousTotal = previousTotal,
    changePercentage = changePercentage
)

// Years, weeks, months
fun AvailableYears.toDto() = AvailableYearsDto(years)
fun AvailableWeeks.toDto() = AvailableWeeksDto(weeks)
fun AvailableMonths.toDto() = AvailableMonthsDto(months)
