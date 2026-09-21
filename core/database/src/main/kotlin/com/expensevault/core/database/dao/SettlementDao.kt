package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements ORDER BY settledAt DESC")
    fun getAll(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE personId = :personId ORDER BY settledAt DESC")
    fun getByPersonId(personId: Long): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settlement: SettlementEntity): Long
}
