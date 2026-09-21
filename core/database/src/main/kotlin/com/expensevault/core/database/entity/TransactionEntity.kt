package com.expensevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensevault.core.model.TransactionSource
import com.expensevault.core.model.TransactionType
import java.math.BigDecimal

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("transactionDate"),
        Index(value = ["type", "transactionDate"]),
        Index(value = ["categoryId", "transactionDate"]),
        Index(value = ["deduplicationKey"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"],
            childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"],
            childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long?,
    val type: TransactionType,
    val originalAmount: BigDecimal,
    val originalCurrency: String,
    val exchangeRate: BigDecimal?,
    val baseAmount: BigDecimal,
    val note: String?,
    val merchant: String?,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationName: String? = null,
    val transactionDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val recurringRuleId: Long? = null,
    val deduplicationKey: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val isEstimatedRate: Boolean = false
)
