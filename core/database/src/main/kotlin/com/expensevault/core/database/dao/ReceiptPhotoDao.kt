package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.ReceiptPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptPhotoDao {
    @Query("SELECT * FROM receipt_photos WHERE transactionId = :transactionId")
    fun getByTransactionId(transactionId: Long): Flow<List<ReceiptPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receiptPhoto: ReceiptPhotoEntity): Long

    @Delete
    suspend fun delete(receiptPhoto: ReceiptPhotoEntity)
}
