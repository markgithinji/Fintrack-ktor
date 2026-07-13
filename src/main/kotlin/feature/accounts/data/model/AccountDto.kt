package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.model.AccountType
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AccountDto(
    val id: String? = null,
    val name: String,
    @Contextual val balance: BigDecimal? = null,
    @Contextual val income: BigDecimal? = null,
    @Contextual val expense: BigDecimal? = null,
    val isDefault: Boolean? = false,
    val type: AccountType? = AccountType.GENERAL,
    val createdAt: Instant? = null,
    val lastSyncedAt: Instant? = null
)
