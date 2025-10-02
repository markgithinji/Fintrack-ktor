package feature.user.domain

import com.fintrack.feature.user.domain.User

interface UserRepository {
    suspend fun createUser(username: String, password: String): Int
    suspend fun findByUsername(username: String): User?
    suspend fun findById(userId: Int): User?
    suspend fun updateUser(userId: Int, username: String? = null, password: String? = null): Boolean
    suspend fun deleteUser(userId: Int): Boolean
    suspend fun userExists(username: String): Boolean
}