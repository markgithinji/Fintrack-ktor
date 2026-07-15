package com.fintrack.feature.user.domain

import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val name: String,
    val passwordHash: String,
    val trackedCategoryIds: List<UUID> = emptyList(),
    val isEmailVerified: Boolean = false
)