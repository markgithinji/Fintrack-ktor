package feature.user.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackedCategoriesRequest(
    val categories: List<String>
)
