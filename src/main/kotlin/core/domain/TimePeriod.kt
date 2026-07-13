package com.fintrack.core.domain

import kotlinx.datetime.*
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

sealed class TimePeriod {
    abstract val year: Int
    
    abstract fun toDateRange(): Pair<LocalDate, LocalDate>
    
    fun matches(instant: Instant, timeZone: TimeZone = TimeZone.UTC): Boolean {
        val dt = instant.toLocalDateTime(timeZone)
        return when (this) {
            is Year -> dt.year == year
            is Month -> (dt.year == year) && (dt.monthNumber == month)
            is Week -> {
                val javaDateTime = dt.toJavaLocalDateTime()
                val w = javaDateTime.get(WeekFields.ISO.weekOfWeekBasedYear())
                (dt.year == year) && (w == week)
            }
        }
    }

    data class Year(override val year: Int) : TimePeriod() {
        override fun toDateRange(): Pair<LocalDate, LocalDate> =
            LocalDate(year, 1, 1) to LocalDate(year, 12, 31)

        override fun toString(): String = year.toString()
    }

    data class Month(override val year: Int, val month: Int) : TimePeriod() {
        init {
            require(month in 1..12) { "Month must be between 1 and 12" }
        }

        override fun toDateRange(): Pair<LocalDate, LocalDate> {
            val start = LocalDate(year, month, 1)
            val end = start.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
            return start to end
        }

        override fun toString(): String = "$year-${month.toString().padStart(2, '0')}"
    }

    data class Week(override val year: Int, val week: Int) : TimePeriod() {
        override fun toDateRange(): Pair<LocalDate, LocalDate> {
            // ISO week calculation
            val calendar = Calendar.getInstance(Locale.ROOT)
            calendar.clear()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.WEEK_OF_YEAR, week)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val javaDate = java.time.LocalDate.ofInstant(calendar.toInstant(), java.time.ZoneId.of("UTC"))
            val start = javaDate.toKotlinLocalDate()
            val end = start.plus(DatePeriod(days = 6))
            return start to end
        }

        override fun toString(): String = "$year-W${week.toString().padStart(2, '0')}"
    }

    companion object {
        fun parse(value: String): TimePeriod {
            return try {
                when {
                    value.contains("-W") -> {
                        val parts = value.split("-W")
                        Week(parts[0].toInt(), parts[1].toInt())
                    }
                    value.contains("-") -> {
                        val parts = value.split("-")
                        Month(parts[0].toInt(), parts[1].toInt())
                    }
                    value.length == 4 -> Year(value.toInt())
                    else -> throw IllegalArgumentException("Invalid period format: $value")
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse TimePeriod: $value", e)
            }
        }
        
        fun fromInstant(instant: Instant, type: String, timeZone: TimeZone = TimeZone.UTC): TimePeriod {
            val dt = instant.toLocalDateTime(timeZone)
            return when (type.lowercase()) {
                "year", "years" -> Year(dt.year)
                "month", "months" -> Month(dt.year, dt.monthNumber)
                "week", "weeks" -> {
                    val javaDateTime = dt.toJavaLocalDateTime()
                    val week = javaDateTime.get(WeekFields.ISO.weekOfWeekBasedYear())
                    Week(javaDateTime.year, week)
                }
                else -> throw IllegalArgumentException("Unknown period type: $type")
            }
        }
    }
}
