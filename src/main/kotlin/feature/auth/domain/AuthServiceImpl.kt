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
        if (userRepository.userExists(email)) {
            throw IllegalArgumentException("User with email '$email' already exists")
        }

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