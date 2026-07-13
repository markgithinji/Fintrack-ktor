package com.fintrack.feature.summary.data.model

import java.math.BigDecimal

data class AccountAggregates(
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO
)
