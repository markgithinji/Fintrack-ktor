package com.fintrack.feature.user.data.model

import com.fintrack.core.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserDto(
    val name: String,
    val email: String,
    val trackedCategoryIds: List<@Serializable(with = UUIDSerializer::class) UUID> = emptyList(),
    val isEmailVerified: Boolean = false
)
