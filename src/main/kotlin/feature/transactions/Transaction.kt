package com.fintrack.feature.transactions

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Int? = null,
    val type: String, // "income" or "expense"
    val amount: Double,
    val category: String,
    val date: String // e.g. "2025-09-02"
)
