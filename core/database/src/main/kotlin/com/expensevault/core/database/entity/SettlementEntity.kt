package com.expensevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "settlements",
    indices = [Index("personId")],
    foreignKeys = [ForeignKey(
        entity = PersonEntity::class, parentColumns = ["id"],
        childColumns = ["personId"], onDelete = ForeignKey.RESTRICT
    )]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val settledAmount: BigDecimal,
    val currency: String,
    val note: String?,
    val settledAt: Long,
    val debtRecordIds: List<Long>
)
