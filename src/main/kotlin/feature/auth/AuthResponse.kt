package feature.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String
)