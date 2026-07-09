package feature.user.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.feature.user.data.model.toDto
import com.fintrack.feature.user.domain.User
import feature.user.data.model.UpdateUserRequest
import feature.user.data.model.UserDto
import com.fintrack.core.EmailService
import feature.auth.domain.repository.EmailVerificationRepository
import feature.auth.domain.model.EmailVerificationToken
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

class UserServiceImpl(
    private val userRepository: UserRepository,
    private val accountsRepository: AccountsRepository,
    private val emailService: EmailService,
    private val emailVerificationRepository: EmailVerificationRepository
) : UserService {

    private val log = logger<UserServiceImpl>()

    override suspend fun getUserProfile(userId: UUID): UserDto {
        log.withContext("userId" to userId).debug { "Fetching user profile" }

        val user = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).warn { "User not found" }
            throw NoSuchElementException("User not found")
        }

        val userDto = user.toDto()

        log.withContext("userId" to userId, "email" to user.email)
            .debug { "User profile retrieved successfully" }
        return userDto
    }

    override suspend fun updateUser(userId: UUID, request: UpdateUserRequest): UserDto {
        log.withContext(
            "userId" to userId,
            "emailUpdate" to (request.email != null),
            "passwordUpdate" to (request.password != null)
        ).info { "Updating user" }

        // Validate user exists
        val existingUser = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).warn { "User update failed - user not found" }
            throw NoSuchElementException("User not found")
        }

        // Check if email is taken
        val updated = if (request.email != null && request.email != existingUser.email) {
            if (userRepository.userExists(request.email)) {
                log.withContext(
                    "userId" to userId,
                    "requestedEmail" to request.email,
                    "currentEmail" to existingUser.email
                ).warn { "User update failed - email already taken" }
                throw IllegalArgumentException("Email '${request.email}' is already taken")
            }

            // Handle email verification flow
            val verificationToken = UUID.randomUUID().toString()
            val token = EmailVerificationToken(
                userId = userId,
                newEmail = request.email,
                token = verificationToken,
                expiresAt = Clock.System.now().plus(24.hours)
            )

            emailVerificationRepository.deleteByUserId(userId) // Revoke any pending ones
            emailVerificationRepository.saveToken(token)
            emailService.sendVerificationEmail(request.email, verificationToken)

            log.withContext("userId" to userId, "newEmail" to request.email).info { "Email change verification requested" }

            // Continue with other updates but DON'T update email yet
            userRepository.updateUser(userId, request.name, null, request.password)
        } else {
            userRepository.updateUser(userId, request.name, request.email, request.password)
        }

        if (!updated) {
            log.withContext("userId" to userId).warn { "User update failed" }
            throw IllegalStateException("Failed to update user")
        }

        val updatedUser = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).warn { "Failed to fetch updated user" }
            throw IllegalStateException("Failed to fetch updated user")
        }

        log.withContext("userId" to userId).info { "User updated successfully" }
        return updatedUser.toDto()
    }

    override suspend fun updateTrackedCategories(userId: UUID, categories: List<String>): UserDto {
        log.withContext("userId" to userId, "categories" to categories).info { "Updating tracked categories" }

        val updated = userRepository.updateTrackedCategories(userId, categories.take(2))

        if (!updated) {
            throw IllegalStateException("Failed to update tracked categories")
        }

        return getUserProfile(userId)
    }

    override suspend fun verifyEmailChange(token: String): UserDto {
        val verificationToken = emailVerificationRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid or expired verification token")

        if (verificationToken.expiresAt < Clock.System.now()) {
            emailVerificationRepository.deleteByToken(token)
            throw IllegalArgumentException("Verification token has expired")
        }

        // Check if email is still available
        if (userRepository.userExists(verificationToken.newEmail)) {
            emailVerificationRepository.deleteByUserId(verificationToken.userId)
            throw IllegalArgumentException("Email '${verificationToken.newEmail}' is no longer available")
        }

        userRepository.updateEmail(verificationToken.userId, verificationToken.newEmail)
        emailVerificationRepository.deleteByUserId(verificationToken.userId)

        log.withContext("userId" to verificationToken.userId, "newEmail" to verificationToken.newEmail)
            .info { "Email verified and updated successfully" }

        return getUserProfile(verificationToken.userId)
    }

    override suspend fun deleteUser(userId: UUID) {
        log.withContext("userId" to userId).warn { "Deleting user" }

        val deleted = userRepository.deleteUser(userId)

        if (!deleted) {
            log.withContext("userId" to userId).warn { "User deletion failed - not found" }
            throw NoSuchElementException("User not found")
        }

        log.withContext("userId" to userId).warn { "User deleted successfully" }
    }

    override suspend fun validateUserCredentials(email: String, password: String): User? {
        log.withContext("email" to email).debug { "Validating user credentials" }

        val user = userRepository.findByEmail(email) ?: run {
            log.withContext("email" to email)
                .debug { "User not found during credential validation" }
            return null
        }

        val isValid = BCrypt.checkpw(password, user.passwordHash)

        if (isValid) {
            log.withContext("userId" to user.id, "email" to email)
                .debug { "User credentials validated successfully" }
        } else {
            log.withContext("userId" to user.id, "email" to email)
                .warn { "Invalid password provided" }
        }

        return if (isValid) user else null
    }

    override suspend fun userExists(email: String): Boolean {
        log.withContext("email" to email).debug { "Checking if user exists" }

        val exists = userRepository.userExists(email)

        log.withContext("email" to email, "exists" to exists)
            .debug { "User existence check completed" }
        return exists
    }
}