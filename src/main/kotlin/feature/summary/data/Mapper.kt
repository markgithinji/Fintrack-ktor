package com.fintrack.feature.summary.data

import com.fintrack.feature.summary.data.model.AvailableMonthsDto
import com.fintrack.feature.summary.data.model.AvailableWeeksDto
import com.fintrack.feature.summary.data.model.AvailableYearsDto
import com.fintrack.feature.summary.domain.CategoryComparison
import com.fintrack.feature.summary.domain.CategorySummary
import com.fintrack.feature.summary.domain.DaySummary
import com.fintrack.feature.summary.domain.DistributionSummary
import com.fintrack.feature.summary.domain.OverviewSummary
import com.fintrack.feature.summary.data.model.CategoryComparisonDto
import com.fintrack.feature.summary.data.model.CategorySummaryDto
import com.fintrack.feature.summary.data.model.DistributionSummaryDto
import core.AvailableMonths
import core.AvailableWeeks
import core.AvailableYears
import core.DaySummaryDto
import core.HighlightDto
import core.HighlightsDto
import core.HighlightsSummaryDto
import core.OverviewSummaryDto
import feature.transactions.Highlight
import feature.transactions.Highlights
import feature.transactions.HighlightsSummary

// Highlights summary
fun HighlightsSummary.toDto(): HighlightsSummaryDto = HighlightsSummaryDto(
    income = income,
    expense = expense,
    balance = balance,
    incomeHighlights = incomeHighlights.toDto(),
    expenseHighlights = expenseHighlights.toDto()
)

fun Highlight.toDto(): HighlightDto =
    HighlightDto(label = label, value = value, amount = amount)

fun Highlights.toDto(): HighlightsDto = HighlightsDto(
    highestMonth = highestMonth?.toDto(),
    highestCategory = highestCategory?.toDto(),
    highestDay = highestDay?.toDto(),
    averagePerDay = averagePerDay
)

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
    weeklyOverview = weeklyOverview.map { it.toDto() },
    monthlyOverview = monthlyOverview.map { it.toDto() }
)

fun DaySummary.toDto() = DaySummaryDto(
    date = date.toString(),
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
