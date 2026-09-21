package com.expensevault.core.database.model

import androidx.room.ColumnInfo

data class CategorySpendTuple(
    @ColumnInfo(name = "categoryId") val categoryId: Long,
    @ColumnInfo(name = "categoryName") val categoryName: String?,
    @ColumnInfo(name = "colorHex") val colorHex: String?,
    @ColumnInfo(name = "total") val total: Double
)
