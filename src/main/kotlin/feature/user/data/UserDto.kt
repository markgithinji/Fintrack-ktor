package com.fintrack.feature.user.data

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val token: String
)