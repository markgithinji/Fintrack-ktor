package com.fintrack.feature.user.data.model

import com.fintrack.feature.user.domain.User
import feature.user.data.model.UserDto

fun User.toDto(): UserDto = UserDto(
    name = this.email,
    email = this.email
)