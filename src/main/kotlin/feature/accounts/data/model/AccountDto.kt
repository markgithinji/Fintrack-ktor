package com.fintrack.feature.accounts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val income: Double,
    val expense: Double,
    val balance: Double
)