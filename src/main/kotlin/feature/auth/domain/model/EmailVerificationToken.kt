package com.fintrack.feature.auth.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class EmailVerificationToken(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val newEmail: String,
    val token: String,
    val expiresAt: Instant
)
