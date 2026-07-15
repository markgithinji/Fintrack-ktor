package com.fintrack.feature.accounts.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID

data class Account(
    val id: UUID? = null,
    val userId: UUID,
    val name: String,
    val isDefault: Boolean = false,
    val type: AccountType = AccountType.GENERAL,
    val linkedSources: Set<String> = emptySet(),
    val balance: BigDecimal = BigDecimal.ZERO,
    val createdAt: Instant? = null,
    val lastSyncedAt: Instant? = null
)

@Serializable
enum class AccountType {
    @SerialName("general")
    GENERAL,

    @SerialName("mpesa")
    MPESA,

    @SerialName("equity")
    EQUITY,

    @SerialName("savings")
    SAVINGS,

    @SerialName("checking")
    CHECKING
}