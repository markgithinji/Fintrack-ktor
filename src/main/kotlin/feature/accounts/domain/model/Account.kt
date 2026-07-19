package com.fintrack.feature.accounts.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.util.UUID

data class Account(
    val id: UUID? = null,
    val userId: UUID,
    val name: String,
    val isDefault: Boolean = false,
    val type: AccountType = AccountType.OTHER,
    val linkedSources: Set<String> = emptySet(),
    val balance: BigDecimal = BigDecimal.ZERO,
    val createdAt: Instant? = null,
    val lastSyncedAt: Instant? = null
)

enum class AccountType {
    OTHER,
    MPESA,
    BANK,
    CASH,
    WALLET,
    SAVINGS
}
