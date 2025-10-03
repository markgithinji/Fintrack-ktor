package feature.user.domain

import feature.user.data.model.UserDto
import com.fintrack.feature.user.domain.User
import feature.user.data.model.CreateUserRequest
import feature.user.data.model.UpdateUserRequest
import org.mindrot.jbcrypt.BCrypt

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

    override suspend fun createUser(request: CreateUserRequest): Int {
        if (userRepository.userExists(request.username)) {
            throw IllegalArgumentException("User with username '${request.username}' already exists")
        }

        return userRepository.createUser(request.username, request.password)
    }

    override suspend fun updateUser(userId: Int, request: UpdateUserRequest): Boolean {
        // Validate user exists
        val existingUser = userRepository.findById(userId) ?: return false

        // Check if username is taken
        if (request.username != null && request.username != existingUser.username) {
            if (userRepository.userExists(request.username)) {
                throw IllegalArgumentException("Username '${request.username}' is already taken")
            }
        }

        return userRepository.updateUser(userId, request.username, request.password)
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