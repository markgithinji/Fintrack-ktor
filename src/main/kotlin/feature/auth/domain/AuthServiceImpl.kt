package feature.auth.domain

import com.fintrack.core.logger
import com.fintrack.core.withContext
import com.fintrack.feature.auth.JwtConfig
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

        log.withContext("userId" to userId, "email" to email).info { "User registered successfully" }
        return AuthResponse(token = token)
    }

    override suspend fun login(email: String, password: String): AuthResponse {
        log.withContext("email" to email).info { "Login attempt" }

        val user = userRepository.findByEmail(email) ?: run {
            log.withContext("email" to email).warn { "Login failed - user not found" }
            throw AuthenticationException("Invalid credentials")
        }

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            log.withContext("userId" to user.id, "email" to email).warn { "Login failed - invalid password" }
            throw AuthenticationException("Invalid credentials")
        }

        val token = JwtConfig.generateToken(user.id)
        log.withContext("userId" to user.id, "email" to email).info { "Login successful" }
        return AuthResponse(token = token)
    }
}