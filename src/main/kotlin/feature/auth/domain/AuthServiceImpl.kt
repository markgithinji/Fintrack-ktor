package feature.auth.domain

import com.fintrack.core.logger
import com.fintrack.core.warn
import com.fintrack.core.withContext
import com.fintrack.feature.auth.JwtConfig
import com.fintrack.feature.auth.domain.AuthValidationResponse
import core.AuthenticationException
import feature.auth.data.model.AuthResponse
import feature.user.domain.UserRepository
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

class AuthServiceImpl(
    private val userRepository: UserRepository
) : AuthService {

    private val log = logger<AuthServiceImpl>()

    override suspend fun register(email: String, password: String): AuthResponse {
        log.withContext("email" to email).info { "Registration attempt" }

        if (userRepository.userExists(email)) {
            log.withContext("email" to email).warn { "Registration failed - user already exists" }
            throw IllegalArgumentException("User with email '$email' already exists")
        }

        val userId = userRepository.createUser(email, password)
        val token = JwtConfig.generateToken(userId)

        log.withContext("userId" to userId, "email" to email)
            .info { "User registered successfully" }
        return AuthResponse(token = token)
    }

    override suspend fun login(email: String, password: String): AuthResponse {
        log.withContext("email" to email).info { "Login attempt" }

        val user = userRepository.findByEmail(email) ?: run {
            log.withContext("email" to email).warn { "Login failed - user not found" }
            throw AuthenticationException("Invalid credentials")
        }

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            log.withContext("userId" to user.id, "email" to email)
                .warn { "Login failed - invalid password" }
            throw AuthenticationException("Invalid credentials")
        }

        val token = JwtConfig.generateToken(user.id)
        log.withContext("userId" to user.id, "email" to email).info { "Login successful" }
        return AuthResponse(token = token)
    }

    override suspend fun validateToken(token: String): AuthValidationResponse {
        log.withContext("tokenLength" to token.length).info { "Token validation attempt" }

        val jwtVerifier = JwtConfig.createVerifier()
        val decodedJWT = jwtVerifier.verify(token)

        val userIdString = decodedJWT.getClaim("userId").asString()

        return if (userIdString != null) {
            val userId = UUID.fromString(userIdString)

            // Check if user still exists in database using findById
            val user = userRepository.findById(userId)
            if (user != null) {
                log.withContext("userId" to userId).info { "Token validation successful" }
                AuthValidationResponse(
                    isValid = true,
                    userId = userId,
                    message = "Token is valid"
                )
            } else {
                log.withContext("userId" to userId)
                    .warn { "Token validation failed - user not found" }
                AuthValidationResponse(
                    isValid = false,
                    userId = null,
                    message = "User no longer exists"
                )
            }
        } else {
            log.warn { "Token validation failed - missing userId claim" }
            AuthValidationResponse(
                isValid = false,
                userId = null,
                message = "Invalid token: missing userId claim"
            )
        }
    }
}