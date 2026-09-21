package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.CategoryEntity
import com.expensevault.core.database.model.CategoryWithSubcategories
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getTopLevel(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Transaction
    @Query("SELECT * FROM categories WHERE parentId IS NULL")
    fun getWithSubcategories(): Flow<List<CategoryWithSubcategories>>
}
