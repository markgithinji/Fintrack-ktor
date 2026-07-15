package com.fintrack.feature.user.data.model

import com.fintrack.core.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TrackedCategoriesRequest(
    val categoryIds: List<@Serializable(with = UUIDSerializer::class) UUID>
)
