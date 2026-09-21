package com.expensevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensevault.core.model.BudgetPeriod
import java.math.BigDecimal

@Entity(
    tableName = "budget_caps",
    indices = [Index(value = ["categoryId", "periodType"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class, parentColumns = ["id"],
        childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE
    )]
)
data class BudgetCapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limitAmount: BigDecimal,
    val currency: String,
    val periodType: BudgetPeriod,
    val warningThreshold: Float = 0.80f,
    val isActive: Boolean = true,
    val createdAt: Long
)
