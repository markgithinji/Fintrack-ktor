package feature.auth.data

import feature.auth.domain.TokenBlacklistService
import redis.clients.jedis.JedisPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RedisTokenBlacklistService(private val jedisPool: JedisPool) : TokenBlacklistService {

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
