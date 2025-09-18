package com.fintrack.feature.user.data

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val name: String,
    val email: String
)