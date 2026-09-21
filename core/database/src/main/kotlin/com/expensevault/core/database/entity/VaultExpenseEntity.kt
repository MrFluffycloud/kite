package com.expensevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "vault_expenses")
data class VaultExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: BigDecimal,
    val currency: String,
    val title: String,
    val note: String?,
    val category: String,
    val date: String,
    val isSharedWithPartner: Boolean,
    val partnerShare: BigDecimal?,
    val createdAt: Long,
    val updatedAt: Long
)
