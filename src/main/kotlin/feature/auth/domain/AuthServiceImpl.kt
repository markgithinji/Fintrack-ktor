package feature.auth.domain

import com.fintrack.core.info
import com.fintrack.core.logger
import com.fintrack.core.warn
import com.fintrack.feature.accounts.domain.Account
import com.fintrack.feature.accounts.domain.AccountsRepository
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.feature.auth.domain.AuthValidationResponse
import core.AuthenticationException
import feature.auth.data.model.AuthResponse
import feature.user.domain.UserRepository
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID
import com.auth0.jwt.JWT
import java.time.LocalDateTime
import java.time.ZoneOffset

class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val accountsRepository: AccountsRepository,
    private val tokenBlacklistService: TokenBlacklistService,
    private val refreshTokenRepository: RefreshTokenRepository
) : AuthService {

    private val log = logger<AuthServiceImpl>()

    override suspend fun register(email: String, password: String): AuthResponse {
        log.info { "Registration attempt for $email" }

        if (userRepository.userExists(email)) {
            log.warn { "Registration failed - user already exists: $email" }
            throw AuthenticationException("User with email '$email' already exists", "USER_ALREADY_EXISTS")
        }

        val userId = userRepository.createUser(email, password)
        createDefaultAccounts(userId)

        return generateAuthResponse(userId)
    }

    override suspend fun login(email: String, password: String): AuthResponse {
        log.info { "Login attempt for $email" }

        val user = userRepository.findByEmail(email) ?: run {
            log.warn { "Login failed - user not found: $email" }
            throw AuthenticationException("Invalid credentials", "INVALID_CREDENTIALS")
        }

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            log.warn { "Login failed - invalid password for: $email" }
            throw AuthenticationException("Invalid credentials", "INVALID_CREDENTIALS")
        }

        return generateAuthResponse(user.id)
    }

    override suspend fun validateToken(token: String): AuthValidationResponse {
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            return AuthValidationResponse(false, null, "Token has been revoked")
        }

        return try {
            val jwtVerifier = JwtConfig.createVerifier()
            val decodedJWT = jwtVerifier.verify(token)
            val userIdString = decodedJWT.getClaim("userId").asString()

            if (userIdString != null) {
                val userId = UUID.fromString(userIdString)
                val user = userRepository.findById(userId)
                if (user != null) {
                    AuthValidationResponse(true, userId, "Token is valid")
                } else {
                    AuthValidationResponse(false, null, "User no longer exists")
                }
            } else {
                AuthValidationResponse(false, null, "Invalid token: missing userId claim")
            }
        } catch (e: Exception) {
            AuthValidationResponse(false, null, "Invalid token: ${e.message}")
        }
    }

    override suspend fun logout(accessToken: String, refreshToken: String?) {
        log.info { "User logging out" }
        
        // 1. Blacklist Access Token
        try {
            val decodedJWT = JWT.decode(accessToken)
            val timeLeft = decodedJWT.expiresAt.time - System.currentTimeMillis()
            if (timeLeft > 0) {
                tokenBlacklistService.blacklistToken(accessToken, timeLeft)
            }
        } catch (e: Exception) {
            log.warn { "Failed to blacklist access token: ${e.message}" }
        }

        // 2. Delete Refresh Token
        refreshToken?.let {
            refreshTokenRepository.deleteByToken(it)
        }
    }

    override suspend fun refreshToken(refreshToken: String): AuthResponse {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: throw AuthenticationException("Invalid refresh token", "INVALID_REFRESH_TOKEN")

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(refreshToken)
            throw AuthenticationException("Refresh token expired", "REFRESH_TOKEN_EXPIRED")
        }

        // Optional: Revoke old refresh token (Token Rotation)
        refreshTokenRepository.deleteByToken(refreshToken)

        return generateAuthResponse(storedToken.userId)
    }

    private suspend fun generateAuthResponse(userId: UUID): AuthResponse {
        val accessToken = JwtConfig.generateAccessToken(userId)
        val refreshTokenString = JwtConfig.generateRefreshToken()
        
        val expiresAt = LocalDateTime.now().plusNanos(JwtConfig.REFRESH_TOKEN_EXPIRATION * 1_000_000)
        
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
        val defaultAccounts = listOf("Bank", "Wallet", "Cash", "Savings")
        defaultAccounts.forEach { accountName ->
            accountsRepository.addAccount(Account(userId = userId, name = accountName))
        }
    }
}
