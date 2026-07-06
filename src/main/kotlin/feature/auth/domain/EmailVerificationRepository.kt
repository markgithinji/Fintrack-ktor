package feature.auth.domain

import java.util.UUID

interface EmailVerificationRepository {
    suspend fun saveToken(token: EmailVerificationToken)
    suspend fun findByToken(token: String): EmailVerificationToken?
    suspend fun deleteByToken(token: String)
    suspend fun deleteByUserId(userId: UUID)
}
