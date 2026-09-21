package com.expensevault.core.database.seed

import com.expensevault.core.database.dao.CategoryDao
import com.expensevault.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class DefaultCategorySeeder(private val categoryDao: CategoryDao) {

    suspend fun seedDefaultCategories() {
        val existing = categoryDao.getTopLevel().firstOrNull() ?: emptyList()
        val now = Clock.System.now().toEpochMilliseconds()

        // Helper to create top-level category with subcategories
        suspend fun createCategoryWithSubs(
            name: String,
            iconName: String,
            colorHex: String,
            sortOrder: Int,
            subcategories: List<String>
        ) {
            val parentId = categoryDao.insert(
                CategoryEntity(
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = true,
                    sortOrder = sortOrder,
                    createdAt = now
                )
            )

            subcategories.forEachIndexed { index, subName ->
                categoryDao.insert(
                    CategoryEntity(
                        name = subName,
                        parentId = parentId,
                        iconName = null,
                        colorHex = null,
                        isDefault = true,
                        sortOrder = index,
                        createdAt = now
                    )
                )
            }
        }

        if (existing.isEmpty()) {
            createCategoryWithSubs(
                "Food", "restaurant", "#FF5722", 0,
                listOf("Groceries", "Dining Out", "Delivery", "Coffee & Snacks")
            )
            createCategoryWithSubs(
                "Transport", "directions_car", "#2196F3", 1,
                listOf("Fuel", "Public Transit", "Cab/Ride", "Parking")
            )
            createCategoryWithSubs(
                "Housing", "home", "#4CAF50", 2,
                listOf("Rent", "Maintenance", "Furniture")
            )
            createCategoryWithSubs(
                "Bills & Utilities", "receipt", "#FFEB3B", 3,
                listOf("Electricity", "Water", "Internet", "Phone", "Gas")
            )
            createCategoryWithSubs(
                "Shopping", "shopping_cart", "#E91E63", 4,
                listOf("Clothing", "Electronics", "Personal Care")
            )
            createCategoryWithSubs(
                "Health", "local_hospital", "#F44336", 5,
                listOf("Medical", "Pharmacy", "Gym & Fitness")
            )
            createCategoryWithSubs(
                "Entertainment", "movie", "#9C27B0", 6,
                listOf("Movies & Shows", "Games", "Subscriptions")
            )
            createCategoryWithSubs(
                "Education", "school", "#3F51B5", 7,
                listOf("Courses", "Books", "Supplies")
            )
            createCategoryWithSubs(
                "Personal", "person", "#607D8B", 8,
                listOf("Gifts", "Charity", "Other")
            )
        }

        // Seed Income categories if missing
        val hasIncomeCategories = existing.any { 
            it.name.equals("Salary", ignoreCase = true) || it.name.equals("Income", ignoreCase = true) 
        }
        if (!hasIncomeCategories) {
            createCategoryWithSubs(
                "Salary", "payments", "#10B981", 100,
                listOf("Monthly Pay", "Bonus", "Overtime")
            )
            createCategoryWithSubs(
                "Freelance", "work", "#06B6D4", 101,
                listOf("Client Work", "Consulting", "Side Projects")
            )
            createCategoryWithSubs(
                "Investments", "trending_up", "#8B5CF6", 102,
                listOf("Dividends", "Interest", "Capital Gains")
            )
            createCategoryWithSubs(
                "Refunds & Cashback", "savings", "#F59E0B", 103,
                listOf("Cashback", "Tax Refund", "Reimbursement")
            )
            createCategoryWithSubs(
                "Other Income", "account_balance", "#3B82F6", 104,
                listOf("Gifts", "Rental Income", "Grants")
            )
        }
    }
}
