package com.expensevault.core.domain.model

import java.math.BigDecimal

data class CategorySpendDisplay(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val colorHex: String
)
