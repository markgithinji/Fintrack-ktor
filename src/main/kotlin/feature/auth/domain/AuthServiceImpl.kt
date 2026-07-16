package com.fintrack.feature.auth.domain

import com.auth0.jwt.JWT
import com.fintrack.core.data.dbQuery
import com.fintrack.core.domain.AppError
import com.fintrack.core.domain.Result
import com.fintrack.core.info
import com.fintrack.core.logger
import com.fintrack.core.warn
import com.fintrack.feature.accounts.domain.model.Account
import com.fintrack.feature.accounts.domain.model.AccountType
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.feature.auth.data.model.AuthResponse
import com.fintrack.feature.auth.domain.model.AuthValidationResponse
import com.fintrack.feature.auth.domain.model.RefreshToken
import com.fintrack.feature.auth.domain.repository.RefreshTokenRepository
import com.fintrack.feature.auth.domain.repository.TokenBlacklistRepository
import com.fintrack.feature.user.domain.UserRepository
import core.PasswordHasher
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val accountsRepository: AccountsRepository,
    private val tokenBlacklistRepository: TokenBlacklistRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) : AuthService {

    private val log = logger<AuthServiceImpl>()

    override suspend fun register(email: String, password: String): Result<AuthResponse> = dbQuery {
        log.info { "Registration attempt for $email" }

        if (userRepository.userExists(email)) {
            log.warn { "Registration failed - user already exists: $email" }
            return@dbQuery Result.Failure(
                AppError.Authentication(
                    "User with email '$email' already exists",
                    "USER_ALREADY_EXISTS"
                )
            )
        }

        val name = email.substringBefore("@")
        val userId = userRepository.createUser(email, password, name)
        createDefaultAccounts(userId)

        log.info { "User registered successfully: $email (userId: $userId)" }
        Result.Success(generateAuthResponse(userId))
    }

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        log.info { "Login attempt for $email" }

        val user = userRepository.findByEmail(email) ?: run {
            log.warn { "Login failed - user not found: $email" }
            return Result.Failure(
                AppError.Authentication(
                    "No account found with this email",
                    "USER_NOT_FOUND"
                )
            )
        }

        if (!PasswordHasher.verify(password, user.passwordHash)) {
            log.warn { "Login failed - invalid password for: $email" }
            return Result.Failure(
                AppError.Authentication(
                    "The password you entered is incorrect",
                    "INVALID_PASSWORD"
                )
            )
        }

        log.info { "Login successful for $email (userId: ${user.id})" }
        return Result.Success(generateAuthResponse(user.id))
    }

    override suspend fun validateToken(token: String): Result<AuthValidationResponse> {
        if (tokenBlacklistRepository.isTokenBlacklisted(token)) {
            log.warn { "Token validation failed - token is blacklisted" }
            return Result.Failure(
                AppError.Authentication(
                    "Token has been revoked",
                    "TOKEN_REVOKED"
                )
            )
        }

        return try {
            val jwtVerifier = JwtConfig.createVerifier()
            val decodedJWT = jwtVerifier.verify(token)
            val userIdString = decodedJWT.getClaim("userId").asString()

            if (userIdString != null) {
                val userId = UUID.fromString(userIdString)
                val user = userRepository.findById(userId)
                if (user != null) {
                    Result.Success(AuthValidationResponse(true, userId, "Token is valid"))
                } else {
                    log.warn { "Token validation failed - user $userId no longer exists" }
                    Result.Failure(AppError.NotFound("User no longer exists"))
                }
            } else {
                log.warn { "Token validation failed - missing userId claim" }
                Result.Failure(
                    AppError.Authentication(
                        "Invalid token: missing userId claim",
                        "INVALID_TOKEN"
                    )
                )
            }
        } catch (e: Exception) {
            log.warn { "Token validation failed - ${e.message}" }
            Result.Failure(AppError.Authentication("Invalid token: ${e.message}", "INVALID_TOKEN"))
        }
    }

    override suspend fun logout(accessToken: String, refreshToken: String?): Result<Unit> {
        log.info { "Logout request received" }

        // 1. Blacklist Access Token
        try {
            val decodedJWT = JWT.decode(accessToken)
            val timeLeft = decodedJWT.expiresAt.time - System.currentTimeMillis()
            if (timeLeft > 0) {
                tokenBlacklistRepository.blacklistToken(accessToken, timeLeft)
                log.info { "Access token blacklisted for the remaining ${timeLeft}ms" }
            }
        } catch (e: Exception) {
            log.warn { "Failed to blacklist access token: ${e.message}" }
        }

        // 2. Delete Refresh Token
        refreshToken?.let {
            refreshTokenRepository.deleteByToken(it)
            log.info { "Refresh token deleted" }
        }

        return Result.Success(Unit)
    }

    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        log.info { "Token refresh requested" }

        val storedToken = refreshTokenRepository.findByToken(refreshToken) ?: run {
            log.warn { "Refresh failed - token not found" }
            return Result.Failure(
                AppError.Authentication(
                    "Invalid refresh token",
                    "INVALID_REFRESH_TOKEN"
                )
            )
        }

        val now = Clock.System.now()

        // Handle Token Reuse (Grace Period)
        if (storedToken.isUsed) {
            val gracePeriod = 30.seconds
            val rotatedAt = storedToken.rotatedAt ?: Instant.DISTANT_PAST

            if (now > rotatedAt + gracePeriod) {
                log.warn { "Refresh failed - token reuse detected after grace period for user ${storedToken.userId}" }
                // Potential attack: Invalidate all tokens for this user
                refreshTokenRepository.deleteByUserId(storedToken.userId)
                return Result.Failure(
                    AppError.Authentication(
                        "Security violation: Refresh token reused",
                        "TOKEN_REUSE_DETECTED"
                    )
                )
            }

            log.info { "Token reuse within grace period for user ${storedToken.userId}. Returning most recent session." }
            // Optional: You could find and return the new token generated from the first rotation,
            // but for simplicity and safety, we allow issuing a new one if it's within the window.
        }

        if (storedToken.expiresAt < now) {
            log.warn { "Refresh failed - token expired for user ${storedToken.userId}" }
            refreshTokenRepository.deleteByToken(refreshToken)
            return Result.Failure(
                AppError.Authentication(
                    "Refresh token expired",
                    "REFRESH_TOKEN_EXPIRED"
                )
            )
        }

        // Revoke old refresh token (Token Rotation with Grace Period)
        refreshTokenRepository.markAsUsed(refreshToken)
        log.info { "Old refresh token rotated for user ${storedToken.userId}" }

        return Result.Success(generateAuthResponse(storedToken.userId))
    }

    override suspend fun changePassword(
        userId: UUID,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        log.info { "Password change attempt for userId: $userId" }

        val user = userRepository.findById(userId) ?: run {
            log.warn { "Password change failed - user not found: $userId" }
            return Result.Failure(AppError.Authentication("User not found", "USER_NOT_FOUND"))
        }

        if (!PasswordHasher.verify(currentPassword, user.passwordHash)) {
            log.warn { "Password change failed - invalid current password for userId: $userId" }
            return Result.Failure(
                AppError.Authentication(
                    "Invalid current password",
                    "INVALID_CREDENTIALS"
                )
            )
        }

        userRepository.updatePassword(userId, newPassword)

        // Invalidate all active refresh tokens to force re-login on all devices
        refreshTokenRepository.deleteByUserId(userId)

        log.info { "Password changed successfully for userId: $userId. All refresh tokens revoked." }
        return Result.Success(Unit)
    }

    private suspend fun generateAuthResponse(userId: UUID): AuthResponse {
        val accessToken = JwtConfig.generateAccessToken(userId)
        val refreshTokenString = JwtConfig.generateRefreshToken()

        val expiresAt = Clock.System.now().plus(JwtConfig.REFRESH_TOKEN_EXPIRATION.milliseconds)

        refreshTokenRepository.save(
            RefreshToken(
                token = refreshTokenString,
                userId = userId,
                expiresAt = expiresAt
            )
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenString
        )
    }

    private suspend fun createDefaultAccounts(userId: UUID) {
        val defaultAccounts = listOf(
            Account(
                userId = userId,
                name = "Mpesa",
                isDefault = true,
                type = AccountType.MPESA,
                createdAt = Instant.parse("2024-01-01T00:00:00Z")
            ),
            Account(
                userId = userId,
                name = "Bank",
                isDefault = true,
                createdAt = Instant.parse("2024-01-01T00:00:01Z")
            ),
            Account(
                userId = userId,
                name = "Wallet",
                isDefault = true,
                createdAt = Instant.parse("2024-01-01T00:00:02Z")
            ),
            Account(
                userId = userId,
                name = "Savings",
                isDefault = true,
                createdAt = Instant.parse("2024-01-01T00:00:03Z")
            ),
            Account(
                userId = userId,
                name = "Cash",
                isDefault = true,
                createdAt = Instant.parse("2024-01-01T00:00:04Z")
            )
        )
        accountsRepository.addAll(defaultAccounts)
    }
}
