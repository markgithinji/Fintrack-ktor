package com.fintrack.feature.auth.domain.model

import core.util.IdGenerator
import kotlinx.datetime.Instant
import java.util.UUID

data class EmailVerificationToken(
    val id: UUID = IdGenerator.nextId(),
    val userId: UUID,
    val newEmail: String,
    val token: String,
    val expiresAt: Instant
)
