package com.expensevault.core.domain.model

import java.math.BigDecimal

data class CategorySpend(
    val categoryId: Long,
    val categoryName: String,
    val totalSpend: BigDecimal,
    val colorHex: String?
)
