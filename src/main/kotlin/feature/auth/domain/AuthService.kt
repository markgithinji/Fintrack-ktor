package feature.auth.domain

import feature.auth.data.model.AuthResponse

interface AuthService {
    suspend fun register(email: String, password: String): AuthResponse
    suspend fun login(email: String, password: String): AuthResponse
}
