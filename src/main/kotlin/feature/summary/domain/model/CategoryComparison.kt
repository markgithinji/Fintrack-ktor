package com.fintrack.feature.summary.domain

data class CategoryComparison(
    val period: String,        // "weekly" or "monthly"
    val category: String,      // category name
    val currentTotal: Double,  // total amount for this period
    val previousTotal: Double, // total amount for last period
    val changePercentage: Double // ((current - previous)/previous)*100
)