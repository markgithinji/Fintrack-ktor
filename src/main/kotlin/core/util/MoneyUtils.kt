package com.fintrack.core.util

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.calculatePercentageChange(previous: BigDecimal): Double =
    if (previous.compareTo(BigDecimal.ZERO) != 0) {
        (this - previous).divide(previous, 4, RoundingMode.HALF_UP).toDouble() * 100
    } else if (this.compareTo(BigDecimal.ZERO) > 0) {
        100.0
    } else {
        0.0
    }

fun BigDecimal.calculateRatio(denominator: BigDecimal): Double? =
    if (denominator > BigDecimal.ZERO) {
        this.divide(denominator, 4, RoundingMode.HALF_UP).toDouble() * 100
    } else null

fun BigDecimal.calculateAverage(count: Int): BigDecimal =
    if (count > 0) {
        this.divide(BigDecimal.valueOf(count.toLong()), 4, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO
