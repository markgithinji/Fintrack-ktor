package com.fintrack.feature.user

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String
)
