package feature.auth.di

import feature.auth.data.RedisTokenBlacklistService
import feature.auth.domain.TokenBlacklistService
import feature.auth.domain.AuthService
import feature.auth.domain.AuthServiceImpl
import org.koin.dsl.module
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

val authModule = module {
    single {
        val host = getPropertyOrNull<String>("redis.host") ?: "localhost"
        val port = getPropertyOrNull<String>("redis.port")?.toInt() ?: 6379
        val password = getPropertyOrNull<String>("redis.password")
        
        if (password.isNullOrBlank()) {
            JedisPool(JedisPoolConfig(), host, port)
        } else {
            JedisPool(JedisPoolConfig(), host, port, 2000, password)
        }
    }
    
    single<TokenBlacklistService> { RedisTokenBlacklistService(get()) }
    single<AuthService> { AuthServiceImpl(get(), get(), get()) }
}
