package feature.user.data.model

data class UpdateUserRequest(
    val email: String? = null,
    val password: String? = null
)