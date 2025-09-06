package com.fintrack.feature.transactions

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual


@Serializable
data class Transaction(
    val id: Int? = null,
    val type: String, // "income" or "expense"
    val amount: Double,
    val category: String,
    @Contextual
    val date: LocalDate // e.g., 2025-09-02
)