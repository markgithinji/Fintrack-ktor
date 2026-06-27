package feature.user.data.model

data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null
)