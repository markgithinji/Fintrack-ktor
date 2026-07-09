package feature.auth.domain

import com.fintrack.core.info
import com.fintrack.core.logger
import com.fintrack.core.warn
import com.fintrack.feature.accounts.domain.Account
import com.fintrack.feature.accounts.domain.AccountType
import com.fintrack.feature.accounts.domain.AccountsRepository
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.feature.auth.domain.AuthValidationResponse
import feature.transaction.domain.CategoryRepository
import feature.transaction.domain.model.Category
import com.fintrack.core.domain.Result
import com.fintrack.core.domain.AppError
import core.dbQuery
import feature.auth.data.model.AuthResponse
import feature.user.domain.UserRepository
import core.PasswordHasher
import java.util.UUID
import com.auth0.jwt.JWT
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val accountsRepository: AccountsRepository,
    private val categoryRepository: CategoryRepository,
    private val tokenBlacklistService: TokenBlacklistService,
    private val refreshTokenRepository: RefreshTokenRepository
) : AuthService {

    private val log = logger<AuthServiceImpl>()

    override suspend fun register(email: String, password: String): Result<AuthResponse> = dbQuery {
        log.info { "Registration attempt for $email" }

        if (userRepository.userExists(email)) {
            log.warn { "Registration failed - user already exists: $email" }
            return@dbQuery Result.Failure(AppError.Authentication("User with email '$email' already exists", "USER_ALREADY_EXISTS"))
        }

        val name = email.substringBefore("@")
        val userId = userRepository.createUser(email, password, name)
        createDefaultAccounts(userId)
        createDefaultCategories(userId)

        Result.Success(generateAuthResponse(userId))
    }

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        log.info { "Login attempt for $email" }

        val user = userRepository.findByEmail(email) ?: run {
            log.warn { "Login failed - user not found: $email" }
            return Result.Failure(AppError.Authentication("No account found with this email", "USER_NOT_FOUND"))
        }

        if (!PasswordHasher.verify(password, user.passwordHash)) {
            log.warn { "Login failed - invalid password for: $email" }
            return Result.Failure(AppError.Authentication("The password you entered is incorrect", "INVALID_PASSWORD"))
        }

        // Migration: If legacy BCrypt hash, update to Argon2
        if (PasswordHasher.isLegacyHash(user.passwordHash)) {
            log.info { "Upgrading legacy password hash for $email" }
            userRepository.updatePassword(user.id, password)
        }

        return Result.Success(generateAuthResponse(user.id))
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

    override suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: return Result.Failure(AppError.Authentication("Invalid refresh token", "INVALID_REFRESH_TOKEN"))

        if (storedToken.expiresAt < Clock.System.now()) {
            refreshTokenRepository.deleteByToken(refreshToken)
            return Result.Failure(AppError.Authentication("Refresh token expired", "REFRESH_TOKEN_EXPIRED"))
        }

        // Optional: Revoke old refresh token (Token Rotation)
        refreshTokenRepository.deleteByToken(refreshToken)

        return Result.Success(generateAuthResponse(storedToken.userId))
    }

    override suspend fun changePassword(userId: UUID, currentPassword: String, newPassword: String): Result<Unit> {
        log.info { "Password change attempt for userId: $userId" }

        val user = userRepository.findById(userId) ?: run {
            log.warn { "Password change failed - user not found: $userId" }
            return Result.Failure(AppError.Authentication("User not found", "USER_NOT_FOUND"))
        }

        if (!PasswordHasher.verify(currentPassword, user.passwordHash)) {
            log.warn { "Password change failed - invalid current password for userId: $userId" }
            return Result.Failure(AppError.Authentication("Invalid current password", "INVALID_CREDENTIALS"))
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
            Account(userId = userId, name = "Mpesa", isDefault = true, type = AccountType.MPESA, createdAt = Instant.parse("2024-01-01T00:00:00Z")),
            Account(userId = userId, name = "Bank", isDefault = true, createdAt = Instant.parse("2024-01-01T00:00:01Z")),
            Account(userId = userId, name = "Wallet", isDefault = true, createdAt = Instant.parse("2024-01-01T00:00:02Z")),
            Account(userId = userId, name = "Savings", isDefault = true, createdAt = Instant.parse("2024-01-01T00:00:03Z")),
            Account(userId = userId, name = "Cash", isDefault = true, createdAt = Instant.parse("2024-01-01T00:00:04Z"))
        )
        accountsRepository.addAll(defaultAccounts)
    }

    private suspend fun createDefaultCategories(userId: UUID) {
        val incomeCategories = listOf(
            "Salary" to "AttachMoney",
            "Freelance" to "Work",
            "Investments" to "TrendingUp",
            "Gifts" to "CardGiftcard",
            "Bonus" to "Paid",
            "Rental" to "RealEstateAgent",
            "Dividends" to "Analytics",
            "Interest" to "Percent",
            "Loans" to "AccountBalanceWallet",
            "Transfer" to "Sync",
            "Savings" to "Savings",
            "Other Income" to "AttachMoney"
        )
        val expenseCategories = listOf(
            "Food" to "Fastfood",
            "Transport" to "DirectionsCar",
            "Shopping" to "ShoppingCart",
            "Health" to "LocalHospital",
            "Bills" to "Receipt",
            "Entertainment" to "Movie",
            "Education" to "School",
            "Gifts" to "CardGiftcard",
            "Travel" to "Flight",
            "Personal Care" to "ContentCut",
            "Subscriptions" to "Subscriptions",
            "Rent" to "Home",
            "Groceries" to "ShoppingBag",
            "Insurance" to "Shield",
            "Dining Out" to "Restaurant",
            "Utilities" to "Lightbulb",
            "Internet" to "Wifi",
            "Airtime" to "Smartphone",
            "Bank" to "AccountBalance",
            "Loans" to "AccountBalanceWallet",
            "Charity" to "VolunteerActivism",
            "Government" to "AccountBalance",
            "Savings" to "Savings",
            "Transfer" to "Sync",
            "Pets" to "Pets",
            "Fitness" to "FitnessCenter",
            "Maintenance" to "Build",
            "Transaction Fees" to "Receipt",
            "Misc" to "HelpOutline"
        )

        val baseTime = Instant.parse("2024-01-01T00:00:00Z")
        var offset = 0
        val categories = incomeCategories.map { (name, icon) ->
            Category(UUID.randomUUID(), userId, name, isExpense = false, iconName = icon, isDefault = true, createdAt = baseTime.plus(kotlin.time.Duration.parse("${offset++}s")))
        } + expenseCategories.map { (name, icon) ->
            Category(UUID.randomUUID(), userId, name, isExpense = true, iconName = icon, isDefault = true, createdAt = baseTime.plus(kotlin.time.Duration.parse("${offset++}s")))
        }

        categoryRepository.addAll(categories)
    }
}
