package feature.auth.domain

import kotlinx.datetime.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID = UUID.randomUUID(),
    val token: String,
    val userId: UUID,
    val expiresAt: Instant
)
