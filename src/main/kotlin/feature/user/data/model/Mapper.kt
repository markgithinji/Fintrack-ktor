package com.fintrack.feature.user.data.model

import com.fintrack.feature.user.domain.User
import feature.user.data.model.UserDto

fun User.toDto(): UserDto = UserDto(
    name = this.name,
    email = this.email,
    trackedCategories = this.trackedCategories?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    isEmailVerified = this.isEmailVerified
)