package com.fintrack.feature.summary.domain

import kotlinx.datetime.LocalDate

data class OverviewSummary(
    val weeklyOverview: List<DaySummary>,
    val monthlyOverview: List<DaySummary>
)

data class DaySummary(
    val date: LocalDate,
    val income: Double,
    val expense: Double
)