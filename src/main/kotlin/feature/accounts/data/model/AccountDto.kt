package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.model.AccountType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String? = null,
    val name: String,
    val balance: Double? = null,
    val income: Double? = null,
    val expense: Double? = null,
    val isDefault: Boolean? = false,
    val type: AccountType? = AccountType.GENERAL,
    val createdAt: Instant? = null,
    val lastSyncedAt: Instant? = null
)
