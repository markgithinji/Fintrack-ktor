package feature.auth.domain.repository

interface TokenBlacklistRepository {
    suspend fun blacklistToken(token: String, expirationTimeMillis: Long)
    suspend fun isTokenBlacklisted(token: String): Boolean
}
