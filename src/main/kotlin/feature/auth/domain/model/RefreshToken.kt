package com.fintrack.feature.auth.domain.model

import core.util.IdGenerator
import kotlinx.datetime.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID = IdGenerator.nextId(),
    val token: String,
    val userId: UUID,
    val expiresAt: Instant,
    val isUsed: Boolean = false,
    val rotatedAt: Instant? = null
)
