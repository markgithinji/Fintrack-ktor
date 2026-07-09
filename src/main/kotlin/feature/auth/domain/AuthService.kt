package feature.auth.domain

import com.fintrack.core.domain.Result
import feature.auth.domain.model.AuthValidationResponse
import feature.auth.data.model.AuthResponse
import java.util.UUID

interface AuthService {
    suspend fun register(email: String, password: String): Result<AuthResponse>
    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun validateToken(token: String): AuthValidationResponse
    suspend fun logout(accessToken: String, refreshToken: String?)
    suspend fun refreshToken(refreshToken: String): Result<AuthResponse>
    suspend fun changePassword(userId: UUID, currentPassword: String, newPassword: String): Result<Unit>
}
