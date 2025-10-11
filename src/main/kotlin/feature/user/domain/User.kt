package com.fintrack.feature.user.domain

import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String
)