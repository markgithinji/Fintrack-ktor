package feature.auth.domain

import com.fintrack.feature.auth.JwtConfig
import core.AuthenticationException
import feature.auth.data.model.AuthResponse
import feature.user.domain.UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthServiceImpl(
    private val userRepository: UserRepository
) : AuthService {

    override suspend fun register(email: String, password: String): AuthResponse {
        // Validate input
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Email and password cannot be empty")
        }

        if (userRepository.userExists(email)) {
            throw IllegalArgumentException("User with email '$email' already exists")
        }

        if (password.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters long")
        }

        // Create user directly via repository
        val userId = userRepository.createUser(email, password)
        val token = JwtConfig.generateToken(userId)
        return AuthResponse(token = token)
    }

    override suspend fun login(email: String, password: String): AuthResponse {
        val user = userRepository.findByUsername(email) ?: throw AuthenticationException("Invalid credentials")

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            throw AuthenticationException("Invalid credentials")
        }

        val token = JwtConfig.generateToken(user.id)
        return AuthResponse(token = token)
    }
}