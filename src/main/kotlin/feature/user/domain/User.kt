package com.fintrack.feature.user.domain

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String
)