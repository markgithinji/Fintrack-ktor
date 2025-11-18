package feature.user.domain

import feature.user.data.model.UserDto
import com.fintrack.feature.user.domain.User
import feature.user.data.model.CreateUserRequest
import feature.user.data.model.UpdateUserRequest

import java.util.UUID

interface UserService {
    suspend fun getUserProfile(userId: UUID): UserDto
    suspend fun updateUser(userId: UUID, request: UpdateUserRequest): UserDto
    suspend fun deleteUser(userId: UUID)
    suspend fun validateUserCredentials(email: String, password: String): User?
    suspend fun userExists(email: String): Boolean
}