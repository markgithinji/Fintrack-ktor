package com.fintrack.feature.user.data.model

import com.fintrack.feature.user.domain.User

fun User.toDto(): UserDto = UserDto(
    name = this.name,
    email = this.email,
    trackedCategoryIds = this.trackedCategoryIds,
    isEmailVerified = this.isEmailVerified
)