package feature.user.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val name: String,
    val email: String
)