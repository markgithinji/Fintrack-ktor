package com.fintrack.feature.auth.domain.repository

import com.fintrack.feature.auth.domain.model.RefreshToken
import java.util.UUID

interface RefreshTokenRepository {
    suspend fun save(refreshToken: RefreshToken)
    suspend fun findByToken(token: String): RefreshToken?
    suspend fun deleteByToken(token: String)
    suspend fun deleteByUserId(userId: UUID)
}
