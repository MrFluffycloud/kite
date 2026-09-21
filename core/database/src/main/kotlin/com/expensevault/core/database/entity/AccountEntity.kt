package com.expensevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensevault.core.model.AccountType
import java.math.BigDecimal

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val defaultCurrency: String,
    val initialBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val iconName: String?,
    val colorHex: String?,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
