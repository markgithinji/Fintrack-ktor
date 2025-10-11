package feature.user.domain

import com.fintrack.feature.user.domain.User

import java.util.UUID

interface UserRepository {
    suspend fun createUser(email: String, password: String): UUID
    suspend fun findByEmail(email: String): User?
    suspend fun findById(userId: UUID): User?
    suspend fun updateUser(userId: UUID, email: String?, password: String?): Boolean
    suspend fun deleteUser(userId: UUID): Boolean
    suspend fun userExists(email: String): Boolean
}