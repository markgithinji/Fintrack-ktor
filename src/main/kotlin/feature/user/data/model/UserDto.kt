package com.fintrack.feature.user.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val name: String,
    val email: String,
    val trackedCategories: List<String> = emptyList(),
    val isEmailVerified: Boolean = false
)