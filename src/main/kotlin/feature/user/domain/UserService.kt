package feature.user.domain

import feature.user.data.model.UserDto
import com.fintrack.feature.user.domain.User

interface UserService {
    suspend fun getUserProfile(userId: Int): UserDto?
    suspend fun createUser(username: String, password: String): Int
    suspend fun updateUser(userId: Int, username: String? = null, password: String? = null): Boolean
    suspend fun deleteUser(userId: Int): Boolean
    suspend fun validateUserCredentials(username: String, password: String): User?
    suspend fun userExists(username: String): Boolean
}