package com.expensevault.core.data.usecase

import com.expensevault.core.database.dao.AccountDao
import com.expensevault.core.database.dao.CategoryDao
import com.expensevault.core.database.dao.TransactionDao
import com.expensevault.core.database.entity.AccountEntity
import com.expensevault.core.database.entity.CategoryEntity
import com.expensevault.core.database.entity.TransactionEntity
import com.expensevault.core.domain.usecase.ImportDataUseCase
import com.expensevault.core.domain.usecase.ImportResult
import com.expensevault.core.model.AccountType
import com.expensevault.core.model.TransactionSource
import com.expensevault.core.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

class ImportDataUseCaseImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ImportDataUseCase {

    override suspend fun import(content: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val trimmed = content.trim()
            if (trimmed.startsWith("{")) {
                importJson(trimmed)
            } else {
                importCsv(trimmed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun importJson(jsonString: String): Result<ImportResult> {
        val root = JSONObject(jsonString)
        val existingAccounts = (accountDao.getAll().firstOrNull() ?: emptyList()).toMutableList()
        val existingCategories = (categoryDao.getAll().firstOrNull() ?: emptyList()).toMutableList()

        val accountIdMap = mutableMapOf<Long, Long>() // oldId -> newId
        val categoryIdMap = mutableMapOf<Long, Long>() // oldId -> newId
        var accountsCreated = 0
        var categoriesCreated = 0

        val now = Clock.System.now().toEpochMilliseconds()

        // 1. Process Accounts
        if (root.has("accounts")) {
            val accountsArray = root.getJSONArray("accounts")
            for (i in 0 until accountsArray.length()) {
                val accObj = accountsArray.getJSONObject(i)
                val oldId = accObj.optLong("id", -1L)
                val name = accObj.getString("name")
                val currency = accObj.optString("currency", "INR")
                val balance = BigDecimal(accObj.optString("balance", "0.00"))

                val match = existingAccounts.find { it.name.equals(name, ignoreCase = true) }
                if (match != null) {
                    if (oldId != -1L) accountIdMap[oldId] = match.id
                } else {
                    val newEntity = AccountEntity(
                        name = name,
                        type = AccountType.BANK_ACCOUNT,
                        defaultCurrency = currency,
                        initialBalance = balance,
                        currentBalance = balance,
                        iconName = "AccountBalance",
                        colorHex = "#111111",
                        createdAt = now,
                        updatedAt = now
                    )
                    val newId = accountDao.insert(newEntity)
                    val saved = newEntity.copy(id = newId)
                    existingAccounts.add(saved)
                    if (oldId != -1L) accountIdMap[oldId] = newId
                    accountsCreated++
                }
            }
        }

        // 2. Process Categories
        if (root.has("categories")) {
            val categoriesArray = root.getJSONArray("categories")
            for (i in 0 until categoriesArray.length()) {
                val catObj = categoriesArray.getJSONObject(i)
                val oldId = catObj.optLong("id", -1L)
                val name = catObj.getString("name")
                val parentId = if (catObj.has("parentId") && !catObj.isNull("parentId")) catObj.getLong("parentId") else null

                val match = existingCategories.find { it.name.equals(name, ignoreCase = true) }
                if (match != null) {
                    if (oldId != -1L) categoryIdMap[oldId] = match.id
                } else {
                    val newEntity = CategoryEntity(
                        name = name,
                        parentId = parentId?.let { categoryIdMap[it] },
                        iconName = "Category",
                        colorHex = "#111111",
                        isDefault = false,
                        sortOrder = existingCategories.size,
                        createdAt = now
                    )
                    val newId = categoryDao.insert(newEntity)
                    val saved = newEntity.copy(id = newId)
                    existingCategories.add(saved)
                    if (oldId != -1L) categoryIdMap[oldId] = newId
                    categoriesCreated++
                }
            }
        }

        // Default fallback account
        val fallbackAccount = existingAccounts.firstOrNull() ?: run {
            val defaultAcc = AccountEntity(
                name = "Main Wallet",
                type = AccountType.WALLET,
                defaultCurrency = "INR",
                initialBalance = BigDecimal.ZERO,
                currentBalance = BigDecimal.ZERO,
                iconName = "AccountBalanceWallet",
                colorHex = "#111111",
                createdAt = now,
                updatedAt = now
            )
            val newId = accountDao.insert(defaultAcc)
            accountsCreated++
            defaultAcc.copy(id = newId)
        }

        // 3. Process Transactions
        var importedCount = 0
        if (root.has("transactions")) {
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val txObj = txArray.getJSONObject(i)
                val dateStr = txObj.getString("date")
                val typeStr = txObj.optString("type", "EXPENSE")
                val originalAmount = BigDecimal(txObj.getString("originalAmount"))
                val originalCurrency = txObj.optString("originalCurrency", "INR")
                val baseAmount = BigDecimal(txObj.optString("baseAmount", originalAmount.toPlainString()))
                val oldAccId = txObj.optLong("accountId", -1L)
                val oldCatId = if (txObj.has("categoryId") && !txObj.isNull("categoryId")) txObj.getLong("categoryId") else null
                val merchant = txObj.optString("merchant", "").takeIf { it.isNotBlank() }
                val note = txObj.optString("note", "").takeIf { it.isNotBlank() }

                val targetAccountId = accountIdMap[oldAccId] ?: fallbackAccount.id
                val targetCategoryId = oldCatId?.let { categoryIdMap[it] }
                val transactionDate = try {
                    LocalDate.parse(dateStr).toEpochDays().toLong()
                } catch (_: Exception) {
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()
                }

                val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.EXPENSE }
                val dedupKey = "import_${transactionDate}_${baseAmount.toPlainString()}_${(merchant ?: note ?: "").lowercase().replace(Regex("[^a-z0-9]"), "")}"

                val existing = transactionDao.getByDeduplicationKey(dedupKey)
                if (existing == null) {
                    val entity = TransactionEntity(
                        accountId = targetAccountId,
                        categoryId = targetCategoryId,
                        type = type,
                        originalAmount = originalAmount,
                        originalCurrency = originalCurrency,
                        exchangeRate = BigDecimal.ONE,
                        baseAmount = baseAmount,
                        note = note,
                        merchant = merchant,
                        transactionDate = transactionDate,
                        createdAt = now,
                        updatedAt = now,
                        deduplicationKey = dedupKey,
                        source = TransactionSource.MANUAL
                    )
                    transactionDao.insert(entity)

                    // Adjust account balance
                    val delta = when (type) {
                        TransactionType.EXPENSE -> baseAmount.negate()
                        TransactionType.INCOME -> baseAmount
                        TransactionType.TRANSFER -> baseAmount.negate()
                    }
                    accountDao.updateBalance(targetAccountId, delta)
                    importedCount++
                }
            }
        }

        return Result.success(
            ImportResult(
                importedCount = importedCount,
                accountsCreated = accountsCreated,
                categoriesCreated = categoriesCreated
            )
        )
    }

    private suspend fun importCsv(csvString: String): Result<ImportResult> {
        val lines = csvString.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return Result.failure(IllegalArgumentException("CSV file is empty"))

        val existingAccounts = (accountDao.getAll().firstOrNull() ?: emptyList()).toMutableList()
        val existingCategories = (categoryDao.getAll().firstOrNull() ?: emptyList()).toMutableList()
        val now = Clock.System.now().toEpochMilliseconds()

        var accountsCreated = 0
        var categoriesCreated = 0
        var importedCount = 0

        val headerTokens = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val dateIdx = headerTokens.indexOfFirst { it.contains("date") }
        val typeIdx = headerTokens.indexOfFirst { it == "type" }
        val amountIdx = headerTokens.indexOfFirst { it == "amount" || it == "baseamount" || it == "debit" }
        val accountIdx = headerTokens.indexOfFirst { it == "account" }
        val categoryIdx = headerTokens.indexOfFirst { it == "category" }
        val merchantIdx = headerTokens.indexOfFirst { it == "merchant" || it == "description" || it == "payee" }
        val noteIdx = headerTokens.indexOfFirst { it == "note" || it == "remarks" }

        val fallbackAccount = existingAccounts.firstOrNull() ?: run {
            val defaultAcc = AccountEntity(
                name = "Main Wallet",
                type = AccountType.WALLET,
                defaultCurrency = "INR",
                initialBalance = BigDecimal.ZERO,
                currentBalance = BigDecimal.ZERO,
                iconName = "AccountBalanceWallet",
                colorHex = "#111111",
                createdAt = now,
                updatedAt = now
            )
            val newId = accountDao.insert(defaultAcc)
            accountsCreated++
            defaultAcc.copy(id = newId)
        }

        for (lineIndex in 1 until lines.size) {
            val row = parseCsvLine(lines[lineIndex])
            if (row.isEmpty()) continue

            val rawAmountStr = if (amountIdx >= 0 && amountIdx < row.size) row[amountIdx].replace(",", "").trim() else ""
            val amount = try { BigDecimal(rawAmountStr) } catch (_: Exception) { continue }
            if (amount <= BigDecimal.ZERO) continue

            val dateStr = if (dateIdx >= 0 && dateIdx < row.size) row[dateIdx].trim() else ""
            val transactionDate = try {
                LocalDate.parse(dateStr).toEpochDays().toLong()
            } catch (_: Exception) {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()
            }

            val typeStr = if (typeIdx >= 0 && typeIdx < row.size) row[typeIdx].trim().uppercase() else "EXPENSE"
            val type = if (typeStr.contains("INC") || typeStr.contains("CRED")) TransactionType.INCOME else TransactionType.EXPENSE

            val accName = if (accountIdx >= 0 && accountIdx < row.size) row[accountIdx].trim() else ""
            val targetAccount = if (accName.isNotBlank()) {
                val found = existingAccounts.find { it.name.equals(accName, ignoreCase = true) }
                if (found != null) found else {
                    val created = AccountEntity(
                        name = accName,
                        type = AccountType.BANK_ACCOUNT,
                        defaultCurrency = "INR",
                        initialBalance = BigDecimal.ZERO,
                        currentBalance = BigDecimal.ZERO,
                        iconName = "AccountBalance",
                        colorHex = "#111111",
                        createdAt = now,
                        updatedAt = now
                    )
                    val newId = accountDao.insert(created)
                    accountsCreated++
                    val saved = created.copy(id = newId)
                    existingAccounts.add(saved)
                    saved
                }
            } else fallbackAccount

            val catName = if (categoryIdx >= 0 && categoryIdx < row.size) row[categoryIdx].trim() else ""
            val targetCategory = if (catName.isNotBlank()) {
                val found = existingCategories.find { it.name.equals(catName, ignoreCase = true) }
                if (found != null) found else {
                    val created = CategoryEntity(
                        name = catName,
                        parentId = null,
                        iconName = "Category",
                        colorHex = "#111111",
                        isDefault = false,
                        sortOrder = existingCategories.size,
                        createdAt = now
                    )
                    val newId = categoryDao.insert(created)
                    categoriesCreated++
                    val saved = created.copy(id = newId)
                    existingCategories.add(saved)
                    saved
                }
            } else null

            val merchant = if (merchantIdx >= 0 && merchantIdx < row.size) row[merchantIdx].trim().takeIf { it.isNotBlank() } else null
            val note = if (noteIdx >= 0 && noteIdx < row.size) row[noteIdx].trim().takeIf { it.isNotBlank() } else null

            val dedupKey = "import_${transactionDate}_${amount.setScale(2, RoundingMode.HALF_UP).toPlainString()}_${(merchant ?: note ?: "").lowercase().replace(Regex("[^a-z0-9]"), "")}"

            val existing = transactionDao.getByDeduplicationKey(dedupKey)
            if (existing == null) {
                val entity = TransactionEntity(
                    accountId = targetAccount.id,
                    categoryId = targetCategory?.id,
                    type = type,
                    originalAmount = amount,
                    originalCurrency = targetAccount.defaultCurrency,
                    exchangeRate = BigDecimal.ONE,
                    baseAmount = amount,
                    note = note,
                    merchant = merchant,
                    transactionDate = transactionDate,
                    createdAt = now,
                    updatedAt = now,
                    deduplicationKey = dedupKey,
                    source = TransactionSource.MANUAL
                )
                transactionDao.insert(entity)

                val delta = when (type) {
                    TransactionType.EXPENSE -> amount.negate()
                    TransactionType.INCOME -> amount
                    TransactionType.TRANSFER -> amount.negate()
                }
                accountDao.updateBalance(targetAccount.id, delta)
                importedCount++
            }
        }

        return Result.success(
            ImportResult(
                importedCount = importedCount,
                accountsCreated = accountsCreated,
                categoriesCreated = categoriesCreated
            )
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
