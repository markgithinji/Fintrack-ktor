package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.model.AccountType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val income: Double,
    val expense: Double,
    val balance: Double,
    val isDefault: Boolean = false,
    val type: AccountType = AccountType.GENERAL,
    val createdAt: Instant? = null
)