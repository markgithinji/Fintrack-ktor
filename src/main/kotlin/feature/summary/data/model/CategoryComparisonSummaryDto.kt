package feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryComparisonSummaryDto(
    val period: String,
    val isCurrent: Boolean = true,
    val data: List<CategoryComparisonDto>
)
