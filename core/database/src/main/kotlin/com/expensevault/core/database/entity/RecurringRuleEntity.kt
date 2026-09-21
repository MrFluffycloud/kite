package com.expensevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensevault.core.model.RecurringFrequency
import java.math.BigDecimal

@Entity(
    tableName = "recurring_rules",
    indices = [Index("accountId"), Index("categoryId")],
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"],
            childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"],
            childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long?,
    val amount: BigDecimal,
    val currency: String,
    val note: String?,
    val frequency: RecurringFrequency,
    val customIntervalDays: Int? = null,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrenceTimestamp: Long,
    val requireConfirmation: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long
)
