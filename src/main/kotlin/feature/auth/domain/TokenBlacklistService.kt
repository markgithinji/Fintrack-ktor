package feature.auth.domain

interface TokenBlacklistService {
    suspend fun blacklistToken(token: String, expirationTimeMillis: Long)
    suspend fun isTokenBlacklisted(token: String): Boolean
}
