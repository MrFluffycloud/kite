package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY nextOccurrenceTimestamp ASC")
    fun getAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: Long): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 ORDER BY nextOccurrenceTimestamp ASC")
    fun getActiveRules(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 AND nextOccurrenceTimestamp <= :currentTimestamp")
    fun getDueRules(currentTimestamp: Long): Flow<List<RecurringRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}
