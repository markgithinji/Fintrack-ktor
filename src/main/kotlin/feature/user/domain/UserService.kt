package feature.user.domain

import feature.user.data.model.UserDto
import com.fintrack.feature.user.domain.User
import feature.user.data.model.CreateUserRequest
import feature.user.data.model.UpdateUserRequest

interface UserService {
    suspend fun getUserProfile(userId: Int): UserDto?
    suspend fun createUser(request: CreateUserRequest): Int
    suspend fun updateUser(userId: Int, request: UpdateUserRequest): Boolean
    suspend fun deleteUser(userId: Int): Boolean
    suspend fun validateUserCredentials(username: String, password: String): User?
    suspend fun userExists(username: String): Boolean
}