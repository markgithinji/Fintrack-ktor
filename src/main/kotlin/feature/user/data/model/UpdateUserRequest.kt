package feature.user.data.model

data class UpdateUserRequest(
    val username: String? = null,
    val password: String? = null
)