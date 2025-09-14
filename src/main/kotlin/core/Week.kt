package core

import kotlinx.serialization.Serializable

// Domain models

data class AvailableWeeks(val weeks: List<String>)
data class AvailableMonths(val months: List<String>)

// DTOs
@Serializable
data class AvailableWeeksDto(val weeks: List<String>)
@Serializable
data class AvailableMonthsDto(val months: List<String>)

// Converters
fun AvailableWeeks.toDto() = AvailableWeeksDto(weeks)
fun AvailableMonths.toDto() = AvailableMonthsDto(months)
