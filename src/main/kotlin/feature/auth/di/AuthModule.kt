package com.fintrack.feature.auth.di

import com.fintrack.feature.auth.data.repository.ExposedRefreshTokenRepository
import com.fintrack.feature.auth.data.repository.ExposedEmailVerificationRepository
import com.fintrack.feature.auth.data.repository.RedisTokenBlacklistRepository
import com.fintrack.feature.auth.domain.repository.TokenBlacklistRepository
import com.fintrack.feature.auth.domain.AuthService
import com.fintrack.feature.auth.domain.AuthServiceImpl
import com.fintrack.feature.auth.domain.repository.RefreshTokenRepository
import com.fintrack.feature.auth.domain.repository.EmailVerificationRepository
import com.fintrack.feature.user.domain.UserRepository
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import feature.transaction.domain.CategoryRepository
import com.fintrack.core.EmailService
import com.fintrack.core.LogEmailService
import org.koin.dsl.module
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

val authModule = module {
    single {
        val host = getPropertyOrNull("redis.host") ?: "localhost"
        val port = getPropertyOrNull<String>("redis.port")?.toInt() ?: 6379
        val password = getPropertyOrNull<String>("redis.password")
        
        if (password.isNullOrBlank()) {
            JedisPool(JedisPoolConfig(), host, port)
        } else {
            JedisPool(JedisPoolConfig(), host, port, 2000, password)
        }
    }
    
    single<TokenBlacklistRepository> { RedisTokenBlacklistRepository(jedisPool = get()) }
    single<RefreshTokenRepository> { ExposedRefreshTokenRepository() }
    single<EmailVerificationRepository> { ExposedEmailVerificationRepository() }
    single<EmailService> { LogEmailService() }
    single<AuthService> {
        AuthServiceImpl(
            userRepository = get<UserRepository>(),
            accountsRepository = get<AccountsRepository>(),
            categoryRepository = get<CategoryRepository>(),
            tokenBlacklistRepository = get<TokenBlacklistRepository>(),
            refreshTokenRepository = get<RefreshTokenRepository>()
        )
    }
}
