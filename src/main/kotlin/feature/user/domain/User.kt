package com.fintrack.feature.user.domain

import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val name: String,
    val passwordHash: String,
    val trackedCategories: String? = null,
    val isEmailVerified: Boolean = false
)