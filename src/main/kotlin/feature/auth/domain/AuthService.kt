package feature.auth.domain

import com.fintrack.feature.auth.domain.AuthValidationResponse
import feature.auth.data.model.AuthResponse

interface AuthService {
    suspend fun register(email: String, password: String): AuthResponse
    suspend fun login(email: String, password: String): AuthResponse
    suspend fun validateToken(token: String): AuthValidationResponse
}