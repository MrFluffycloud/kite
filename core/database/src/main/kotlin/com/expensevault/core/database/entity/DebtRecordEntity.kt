package com.expensevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.DebtStatus
import com.expensevault.core.model.SplitMethod
import java.math.BigDecimal

@Entity(
    tableName = "debt_records",
    indices = [
        Index("personId"),
        Index("transactionId"),
        Index(value = ["status", "personId"])
    ],
    foreignKeys = [
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"],
            childColumns = ["personId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"],
            childColumns = ["transactionId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class DebtRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val transactionId: Long? = null,
    val amount: BigDecimal,
    val currency: String,
    val direction: DebtDirection,
    val status: DebtStatus,
    val splitMethod: SplitMethod?,
    val note: String?,
    val createdAt: Long,
    val settledAt: Long? = null
)
