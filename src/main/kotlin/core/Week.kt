package core

import kotlinx.serialization.Serializable

// Domain model
data class AvailableWeeks(
    val weeks: List<String>
)

@Serializable
data class AvailableWeeksDto(
    val weeks: List<String>
)

fun AvailableWeeks.toDto() = AvailableWeeksDto(weeks)
