package com.expensevault.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.expensevault.core.database.entity.CategoryEntity

data class CategoryWithSubcategories(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val subcategories: List<CategoryEntity>
)
