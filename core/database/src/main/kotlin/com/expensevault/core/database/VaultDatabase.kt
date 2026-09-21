package com.expensevault.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.expensevault.core.database.converter.Converters
import com.expensevault.core.database.dao.VaultExpenseDao
import com.expensevault.core.database.entity.VaultExpenseEntity

@Database(
    entities = [VaultExpenseEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultExpenseDao(): VaultExpenseDao
}
