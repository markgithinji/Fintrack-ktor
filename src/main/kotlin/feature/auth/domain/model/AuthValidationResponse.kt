package feature.auth.domain.model

import com.fintrack.core.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AuthValidationResponse(
    val isValid: Boolean,
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID?,
    val message: String
)
