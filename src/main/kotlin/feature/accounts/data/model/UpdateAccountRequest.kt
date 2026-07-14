package com.fintrack.feature.accounts.data.model

import com.fintrack.feature.accounts.domain.model.AccountType
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UpdateAccountRequest(
    val name: String,
    val type: AccountType = AccountType.GENERAL,
    @Contextual val balance: BigDecimal? = null,
    val lastSyncedAt: Instant? = null
)