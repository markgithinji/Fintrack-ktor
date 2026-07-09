package feature.auth.data.repository

import feature.auth.domain.repository.TokenBlacklistRepository
import redis.clients.jedis.JedisPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RedisTokenBlacklistRepository(private val jedisPool: JedisPool) : TokenBlacklistRepository {

    override suspend fun blacklistToken(token: String, expirationTimeMillis: Long) = withContext(Dispatchers.IO) {
        jedisPool.resource.use { jedis ->
            val ttlSeconds = (expirationTimeMillis / 1000).toInt()
            if (ttlSeconds > 0) {
                jedis.setex("blacklist:$token", ttlSeconds.toLong(), "1")
            }
        }
    }

    override suspend fun isTokenBlacklisted(token: String): Boolean = withContext(Dispatchers.IO) {
        jedisPool.resource.use { jedis ->
            jedis.exists("blacklist:$token")
        }
    }
}
