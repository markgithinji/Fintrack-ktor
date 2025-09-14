package core

import kotlinx.serialization.Serializable

// Domain models

data class AvailableWeeks(val weeks: List<String>)
data class AvailableMonths(val months: List<String>)
data class AvailableYears(val years: List<String>)

// DTOs
@Serializable
data class AvailableWeeksDto(val weeks: List<String>)
@Serializable
data class AvailableMonthsDto(val months: List<String>)
@Serializable
data class AvailableYearsDto(val years: List<String>)

// Converters
fun AvailableYears.toDto() = AvailableYearsDto(years)
fun AvailableWeeks.toDto() = AvailableWeeksDto(weeks)
fun AvailableMonths.toDto() = AvailableMonthsDto(months)
