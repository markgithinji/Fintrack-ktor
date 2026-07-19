package com.fintrack.feature.accounts.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UpdateAccountRequest(
    val name: String,
    val type: AccountTypeDto = AccountTypeDto.OTHER,
    val linkedSources: Set<String>? = null,
    @Contextual val balance: BigDecimal? = null,
    val lastSyncedAt: Instant? = null
)
