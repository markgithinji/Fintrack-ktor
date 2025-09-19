package com.fintrack.feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AvailableWeeksDto(val weeks: List<String>)
@Serializable
data class AvailableMonthsDto(val months: List<String>)
@Serializable
data class AvailableYearsDto(val years: List<String>)