package com.fintrack.feature.accounts.domain

import kotlinx.datetime.Instant
import java.util.UUID

data class Account(
    val id: UUID? = null,
    val userId: UUID,
    val name: String,
    val isDefault: Boolean = false,
    val isMpesa: Boolean = false,
    val createdAt: Instant? = null
)