package feature.auth.domain.repository

import feature.auth.domain.model.EmailVerificationToken
import java.util.UUID

interface EmailVerificationRepository {
    suspend fun saveToken(token: EmailVerificationToken)
    suspend fun findByToken(token: String): EmailVerificationToken?
    suspend fun deleteByToken(token: String)
    suspend fun deleteByUserId(userId: UUID)
}
