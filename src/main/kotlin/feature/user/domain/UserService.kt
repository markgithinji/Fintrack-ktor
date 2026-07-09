package feature.user.domain

import com.fintrack.core.domain.Result
import feature.user.data.model.UserDto
import com.fintrack.feature.user.domain.User
import feature.user.data.model.UpdateUserRequest
import java.util.UUID

interface UserService {
    suspend fun getUserProfile(userId: UUID): Result<UserDto>
    suspend fun updateUser(userId: UUID, request: UpdateUserRequest): Result<UserDto>
    suspend fun updateTrackedCategories(userId: UUID, categories: List<String>): Result<UserDto>
    suspend fun verifyEmailChange(token: String): Result<UserDto>
    suspend fun deleteUser(userId: UUID): Result<Unit>
    suspend fun validateUserCredentials(email: String, password: String): User?
    suspend fun userExists(email: String): Boolean
}
