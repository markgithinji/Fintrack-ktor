package feature.user.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import feature.user.data.model.UserDto
import com.fintrack.feature.user.domain.User
import feature.user.data.model.CreateUserRequest
import feature.user.data.model.UpdateUserRequest
import org.mindrot.jbcrypt.BCrypt
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    private val log = logger<UserServiceImpl>()

    override suspend fun getUserProfile(userId: Int): UserDto? {
        log.withContext("userId" to userId).debug { "Fetching user profile" }

        val user = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).debug { "User not found" }
            return null
        }

        val userDto = UserDto(
            name = user.username,
            email = user.username
        )

        log.withContext("userId" to userId, "username" to user.username)
            .debug { "User profile retrieved successfully" }
        return userDto
    }

    override suspend fun createUser(request: CreateUserRequest): Int {
        log.withContext("username" to request.username)
            .info { "Creating user" }

        if (userRepository.userExists(request.username)) {
            log.withContext("username" to request.username)
                .warn { "User creation failed - username already exists" }
            throw IllegalArgumentException("User with username '${request.username}' already exists")
        }

        val userId = userRepository.createUser(request.username, request.password)

        log.withContext("userId" to userId, "username" to request.username)
            .info { "User created successfully" }
        return userId
    }

    override suspend fun updateUser(userId: Int, request: UpdateUserRequest): Boolean {
        log.withContext(
            "userId" to userId,
            "usernameUpdate" to (request.username != null),
            "passwordUpdate" to (request.password != null)
        ).info { "Updating user" }

        // Validate user exists
        val existingUser = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).warn { "User update failed - user not found" }
            return false
        }

        // Check if username is taken
        if (request.username != null && request.username != existingUser.username) {
            if (userRepository.userExists(request.username)) {
                log.withContext(
                    "userId" to userId,
                    "requestedUsername" to request.username,
                    "currentUsername" to existingUser.username
                ).warn { "User update failed - username already taken" }
                throw IllegalArgumentException("Username '${request.username}' is already taken")
            }
        }

        val updated = userRepository.updateUser(userId, request.username, request.password)

        if (updated) {
            log.withContext("userId" to userId).info { "User updated successfully" }
        } else {
            log.withContext("userId" to userId).warn { "User update failed" }
        }

        return updated
    }

    override suspend fun deleteUser(userId: Int): Boolean {
        log.withContext("userId" to userId).warn { "Deleting user" } // Warn level for destructive operation

        val deleted = userRepository.deleteUser(userId)

        if (deleted) {
            log.withContext("userId" to userId).warn { "User deleted successfully" }
        } else {
            log.withContext("userId" to userId).warn { "User deletion failed - not found" }
        }

        return deleted
    }

    override suspend fun validateUserCredentials(username: String, password: String): User? {
        log.withContext("username" to username).debug { "Validating user credentials" }

        val user = userRepository.findByUsername(username) ?: run {
            log.withContext("username" to username).debug { "User not found during credential validation" }
            return null
        }

        val isValid = BCrypt.checkpw(password, user.passwordHash)

        if (isValid) {
            log.withContext("userId" to user.id, "username" to username)
                .debug { "User credentials validated successfully" }
        } else {
            log.withContext("userId" to user.id, "username" to username)
                .warn { "Invalid password provided" }
        }

        return if (isValid) user else null
    }

    override suspend fun userExists(username: String): Boolean {
        log.withContext("username" to username).debug { "Checking if user exists" }

        val exists = userRepository.userExists(username)

        log.withContext("username" to username, "exists" to exists)
            .debug { "User existence check completed" }
        return exists
    }
}