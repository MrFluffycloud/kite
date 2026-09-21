package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.DebtRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtRecordDao {
    @Query("SELECT * FROM debt_records ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DebtRecordEntity>>

    @Query("SELECT * FROM debt_records WHERE personId = :personId ORDER BY createdAt DESC")
    fun getByPersonId(personId: Long): Flow<List<DebtRecordEntity>>

    @Query("SELECT * FROM debt_records WHERE status = 'OPEN' ORDER BY createdAt DESC")
    fun getOpenDebts(): Flow<List<DebtRecordEntity>>

    @Query("SELECT * FROM debt_records WHERE personId = :personId")
    suspend fun getNetBalanceByPerson(personId: Long): List<DebtRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debtRecord: DebtRecordEntity): Long

    @Update
    suspend fun update(debtRecord: DebtRecordEntity)

    @Query("SELECT * FROM debt_records WHERE id = :id")
    suspend fun getById(id: Long): DebtRecordEntity?

    @Query("DELETE FROM debt_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE debt_records SET status = 'SETTLED', settledAt = :settledAt WHERE id = :id")
    suspend fun markAsSettled(id: Long, settledAt: Long)
}
