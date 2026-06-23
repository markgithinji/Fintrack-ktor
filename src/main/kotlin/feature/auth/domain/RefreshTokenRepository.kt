package feature.auth.domain

import java.util.UUID

interface RefreshTokenRepository {
    suspend fun save(refreshToken: RefreshToken)
    suspend fun findByToken(token: String): RefreshToken?
    suspend fun deleteByToken(token: String)
    suspend fun deleteByUserId(userId: UUID)
}
