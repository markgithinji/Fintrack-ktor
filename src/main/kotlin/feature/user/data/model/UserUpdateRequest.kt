package feature.user.data.model

data class UserUpdateRequest(
    val username: String? = null,
    val password: String? = null
)