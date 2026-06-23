package feature.auth.domain

import java.time.LocalDateTime
import java.util.UUID

data class RefreshToken(
    val id: UUID = UUID.randomUUID(),
    val token: String,
    val userId: UUID,
    val expiresAt: LocalDateTime
)
