package feature.auth.domain

import com.fintrack.feature.auth.domain.AuthValidationResponse
import feature.auth.data.model.AuthResponse
import java.util.UUID

interface AuthService {
    suspend fun register(email: String, password: String): AuthResponse
    suspend fun login(email: String, password: String): AuthResponse
    suspend fun validateToken(token: String): AuthValidationResponse
    suspend fun logout(accessToken: String, refreshToken: String?)
    suspend fun refreshToken(refreshToken: String): AuthResponse
    suspend fun changePassword(userId: UUID, currentPassword: String, newPassword: String)
}
