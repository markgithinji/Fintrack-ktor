package feature.user.domain

import com.fintrack.core.EmailService
import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.feature.user.data.model.toDto
import com.fintrack.feature.user.domain.User
import feature.auth.domain.model.EmailVerificationToken
import feature.auth.domain.repository.EmailVerificationRepository
import feature.user.data.model.UpdateUserRequest
import feature.user.data.model.UserDto
import kotlinx.datetime.Clock
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID
import kotlin.time.Duration.Companion.hours

class UserServiceImpl(
    private val userRepository: UserRepository,
    private val accountsRepository: AccountsRepository,
    private val emailService: EmailService,
    private val emailVerificationRepository: EmailVerificationRepository
) : UserService {

    private val log = logger<UserServiceImpl>()

    override suspend fun getUserProfile(userId: UUID): Result<UserDto> {
        log.withContext("userId" to userId).debug { "Fetching user profile" }

        val user = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).warn { "User not found" }
            return Result.Failure(AppError.NotFound("User not found"))
        }

        val userDto = user.toDto()

        log.withContext("userId" to userId, "email" to user.email)
            .debug { "User profile retrieved successfully" }
        return Result.Success(userDto)
    }

    override suspend fun updateUser(userId: UUID, request: UpdateUserRequest): Result<UserDto> {
        log.withContext(
            "userId" to userId,
            "emailUpdate" to (request.email != null),
            "passwordUpdate" to (request.password != null)
        ).info { "Updating user" }

        // Validate user exists
        val existingUser = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).warn { "User update failed - user not found" }
            return Result.Failure(AppError.NotFound("User not found"))
        }

        // Check if email is taken
        val updated = if (request.email != null && request.email != existingUser.email) {
            if (userRepository.userExists(request.email)) {
                log.withContext(
                    "userId" to userId,
                    "requestedEmail" to request.email,
                    "currentEmail" to existingUser.email
                ).warn { "User update failed - email already taken" }
                return Result.Failure(AppError.Conflict("Email '${request.email}' is already taken"))
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
            return Result.Failure(AppError.Internal("Failed to update user"))
        }

        val updatedUser = userRepository.findById(userId) ?: run {
            log.withContext("userId" to userId).warn { "Failed to fetch updated user" }
            return Result.Failure(AppError.Internal("Failed to fetch updated user"))
        }

        log.withContext("userId" to userId).info { "User updated successfully" }
        return Result.Success(updatedUser.toDto())
    }

    override suspend fun updateTrackedCategories(userId: UUID, categories: List<String>): Result<UserDto> {
        log.withContext("userId" to userId, "categories" to categories).info { "Updating tracked categories" }

        val updated = userRepository.updateTrackedCategories(userId, categories.take(2))

        if (!updated) {
            return Result.Failure(AppError.Internal("Failed to update tracked categories"))
        }

        return getUserProfile(userId)
    }

    override suspend fun verifyEmailChange(token: String): Result<UserDto> {
        val verificationToken = emailVerificationRepository.findByToken(token)
            ?: return Result.Failure(AppError.Authentication("Invalid or expired verification token"))

        if (verificationToken.expiresAt < Clock.System.now()) {
            emailVerificationRepository.deleteByToken(token)
            return Result.Failure(AppError.Authentication("Verification token has expired"))
        }

        // Check if email is still available
        if (userRepository.userExists(verificationToken.newEmail)) {
            emailVerificationRepository.deleteByUserId(verificationToken.userId)
            return Result.Failure(AppError.Conflict("Email '${verificationToken.newEmail}' is no longer available"))
        }

        userRepository.updateEmail(verificationToken.userId, verificationToken.newEmail)
        emailVerificationRepository.deleteByUserId(verificationToken.userId)

        log.withContext("userId" to verificationToken.userId, "newEmail" to verificationToken.newEmail)
            .info { "Email verified and updated successfully" }

        return getUserProfile(verificationToken.userId)
    }

    override suspend fun deleteUser(userId: UUID): Result<Unit> {
        log.withContext("userId" to userId).warn { "Deleting user" }

        val deleted = userRepository.deleteUser(userId)

        if (!deleted) {
            log.withContext("userId" to userId).warn { "User deletion failed - not found" }
            return Result.Failure(AppError.NotFound("User not found"))
        }

        log.withContext("userId" to userId).warn { "User deleted successfully" }
        return Result.Success(Unit)
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
