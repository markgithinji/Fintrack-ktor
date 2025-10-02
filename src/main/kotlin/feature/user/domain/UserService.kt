package feature.user.domain

import com.fintrack.feature.user.data.UserDto
import com.fintrack.feature.user.data.UserRepository
import com.fintrack.feature.user.domain.User
import org.mindrot.jbcrypt.BCrypt

interface UserService {
    suspend fun getUserProfile(userId: Int): UserDto?
    suspend fun createUser(username: String, password: String): Int
    suspend fun updateUser(userId: Int, username: String? = null, password: String? = null): Boolean
    suspend fun deleteUser(userId: Int): Boolean
    suspend fun validateUserCredentials(username: String, password: String): User?
    suspend fun userExists(username: String): Boolean
}

class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    override suspend fun getUserProfile(userId: Int): UserDto? {
        val user = userRepository.findById(userId) ?: return null
        return UserDto(
            name = user.username,
            email = user.username
        )
    }

    override suspend fun createUser(username: String, password: String): Int {
        // Validate input
        if (username.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Username and password cannot be empty")
        }

        if (userRepository.userExists(username)) {
            throw IllegalArgumentException("User with username '$username' already exists")
        }

        // Validate password strength
        if (password.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters long")
        }

        return userRepository.createUser(username, password)
    }

    override suspend fun updateUser(userId: Int, username: String?, password: String?): Boolean {
        // Validate user exists
        val existingUser = userRepository.findById(userId) ?: return false

        // Validate new username if provided
        if (username != null) {
            if (username.isBlank()) {
                throw IllegalArgumentException("Username cannot be empty")
            }
            if (username != existingUser.username && userRepository.userExists(username)) {
                throw IllegalArgumentException("Username '$username' is already taken")
            }
        }

        // Validate new password if provided
        if (password != null && password.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters long")
        }

        return userRepository.updateUser(userId, username, password)
    }

    override suspend fun deleteUser(userId: Int): Boolean {
        return userRepository.deleteUser(userId)
    }

    override suspend fun validateUserCredentials(username: String, password: String): User? {
        val user = userRepository.findByUsername(username) ?: return null

        return if (BCrypt.checkpw(password, user.passwordHash)) {
            user
        } else {
            null
        }
    }

    override suspend fun userExists(username: String): Boolean {
        return userRepository.userExists(username)
    }
}