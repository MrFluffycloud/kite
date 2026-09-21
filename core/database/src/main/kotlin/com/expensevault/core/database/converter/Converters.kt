package com.expensevault.core.database.converter

import androidx.room.TypeConverter
import com.expensevault.core.model.*
import java.math.BigDecimal

class Converters {
    // BigDecimal <-> String
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()
    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    // AccountType
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name
    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    // TransactionType
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    // TransactionSource
    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String = value.name
    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)

    // DebtDirection
    @TypeConverter
    fun fromDebtDirection(value: DebtDirection): String = value.name
    @TypeConverter
    fun toDebtDirection(value: String): DebtDirection = DebtDirection.valueOf(value)

    // DebtStatus
    @TypeConverter
    fun fromDebtStatus(value: DebtStatus): String = value.name
    @TypeConverter
    fun toDebtStatus(value: String): DebtStatus = DebtStatus.valueOf(value)

    // SplitMethod
    @TypeConverter
    fun fromSplitMethod(value: SplitMethod?): String? = value?.name
    @TypeConverter
    fun toSplitMethod(value: String?): SplitMethod? = value?.let { SplitMethod.valueOf(it) }

    // RecurringFrequency
    @TypeConverter
    fun fromRecurringFrequency(value: RecurringFrequency): String = value.name
    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency = RecurringFrequency.valueOf(value)

    // BudgetPeriod
    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String = value.name
    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod = BudgetPeriod.valueOf(value)

    // List<Long> for settlement debtRecordIds — store as comma-separated string
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? = value?.joinToString(",")
    @TypeConverter
    fun toLongList(value: String?): List<Long>? = value?.split(",")?.mapNotNull { it.toLongOrNull() }
}
