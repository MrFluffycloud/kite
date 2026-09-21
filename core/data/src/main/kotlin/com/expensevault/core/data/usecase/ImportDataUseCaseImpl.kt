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

import com.expensevault.core.database.dao.DebtRecordDao
import com.expensevault.core.database.dao.PersonDao
import com.expensevault.core.database.dao.RecurringRuleDao
import com.expensevault.core.database.entity.DebtRecordEntity
import com.expensevault.core.database.entity.PersonEntity
import com.expensevault.core.database.entity.RecurringRuleEntity
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.DebtStatus
import com.expensevault.core.model.RecurringFrequency
import com.expensevault.core.model.SplitMethod

class ImportDataUseCaseImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val personDao: PersonDao,
    private val debtRecordDao: DebtRecordDao,
    private val recurringRuleDao: RecurringRuleDao
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
        val transactionIdMap = mutableMapOf<Long, Long>() // oldId -> newId
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
                val txId = if (existing == null) {
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
                    val insertedId = transactionDao.insert(entity)

                    // Adjust account balance
                    val delta = when (type) {
                        TransactionType.EXPENSE -> baseAmount.negate()
                        TransactionType.INCOME -> baseAmount
                        TransactionType.TRANSFER -> baseAmount.negate()
                    }
                    accountDao.updateBalance(targetAccountId, delta)
                    importedCount++
                    insertedId
                } else {
                    existing.id
                }
                val oldTxId = txObj.optLong("id", -1L)
                if (oldTxId != -1L) transactionIdMap[oldTxId] = txId
            }
        }

        // 4. Process Persons
        val personIdMap = mutableMapOf<Long, Long>() // oldPersonId -> newPersonId
        var personsCreated = 0
        val existingPersons = (personDao.getAll().firstOrNull() ?: emptyList()).toMutableList()
        if (root.has("persons")) {
            val personsArray = root.getJSONArray("persons")
            for (i in 0 until personsArray.length()) {
                val pObj = personsArray.getJSONObject(i)
                val oldPersonId = pObj.optLong("id", -1L)
                val name = pObj.getString("name")
                val phone = pObj.optString("phone", "").takeIf { it.isNotBlank() }
                val avatar = pObj.optString("avatarPath", "").takeIf { it.isNotBlank() }

                val match = existingPersons.find { it.name.equals(name, ignoreCase = true) }
                if (match != null) {
                    if (oldPersonId != -1L) personIdMap[oldPersonId] = match.id
                } else {
                    val entity = PersonEntity(
                        name = name,
                        phone = phone,
                        avatarPath = avatar,
                        createdAt = now
                    )
                    val newId = personDao.insert(entity)
                    existingPersons.add(entity.copy(id = newId))
                    if (oldPersonId != -1L) personIdMap[oldPersonId] = newId
                    personsCreated++
                }
            }
        }

        // 5. Process Debts
        var debtsCreated = 0
        if (root.has("debts")) {
            val debtsArray = root.getJSONArray("debts")
            val existingDebts = (debtRecordDao.getAll().firstOrNull() ?: emptyList()).toMutableList()
            for (i in 0 until debtsArray.length()) {
                val dObj = debtsArray.getJSONObject(i)
                val oldPersonId = dObj.getLong("personId")
                val targetPersonId = personIdMap[oldPersonId] ?: continue

                val oldTxId = dObj.optLong("transactionId", -1L).takeIf { it != -1L }
                val targetTxId = oldTxId?.let { transactionIdMap[it] }

                val amount = BigDecimal(dObj.getString("amount"))
                val currency = dObj.optString("currency", "INR")
                val directionStr = dObj.optString("direction", "THEY_OWE_ME")
                val direction = try { DebtDirection.valueOf(directionStr) } catch (_: Exception) { DebtDirection.THEY_OWE_ME }
                val statusStr = dObj.optString("status", "OPEN")
                val status = try { DebtStatus.valueOf(statusStr) } catch (_: Exception) { DebtStatus.OPEN }
                val splitMethodStr = dObj.optString("splitMethod", "")
                val splitMethod = try { SplitMethod.valueOf(splitMethodStr) } catch (_: Exception) { null }
                val note = dObj.optString("note", "").takeIf { it.isNotBlank() }
                val createdAt = dObj.optLong("createdAt", now)
                val settledAt = if (dObj.has("settledAt") && !dObj.isNull("settledAt")) dObj.getLong("settledAt") else null

                // Check for duplicate debt
                val isDuplicate = existingDebts.any { 
                    it.personId == targetPersonId && 
                    it.amount.compareTo(amount) == 0 && 
                    it.direction == direction && 
                    it.note == note 
                }

                if (!isDuplicate) {
                    val debtEntity = DebtRecordEntity(
                        personId = targetPersonId,
                        transactionId = targetTxId,
                        amount = amount,
                        currency = currency,
                        direction = direction,
                        status = status,
                        splitMethod = splitMethod,
                        note = note,
                        createdAt = createdAt,
                        settledAt = settledAt
                    )
                    debtRecordDao.insert(debtEntity)
                    debtsCreated++
                }
            }
        }

        // 6. Process Recurring Rules
        var recurringRulesCreated = 0
        if (root.has("recurringRules")) {
            val rulesArray = root.getJSONArray("recurringRules")
            val existingRules = (recurringRuleDao.getAll().firstOrNull() ?: emptyList()).toMutableList()
            for (i in 0 until rulesArray.length()) {
                val rObj = rulesArray.getJSONObject(i)
                val oldAccId = rObj.optLong("accountId", -1L)
                val targetAccountId = accountIdMap[oldAccId] ?: fallbackAccount.id
                val oldCatId = if (rObj.has("categoryId") && !rObj.isNull("categoryId")) rObj.getLong("categoryId") else null
                val targetCategoryId = oldCatId?.let { categoryIdMap[it] }

                val amount = BigDecimal(rObj.getString("amount"))
                val currency = rObj.optString("currency", "INR")
                val note = rObj.optString("note", "").takeIf { it.isNotBlank() }
                val freqStr = rObj.optString("frequency", "MONTHLY")
                val frequency = try { RecurringFrequency.valueOf(freqStr) } catch (_: Exception) { RecurringFrequency.MONTHLY }
                val dayOfMonth = if (rObj.has("dayOfMonth") && !rObj.isNull("dayOfMonth")) rObj.getInt("dayOfMonth") else null
                val startDate = rObj.optLong("startDate", now)
                val nextTimestamp = rObj.optLong("nextOccurrenceTimestamp", now)
                val isActive = rObj.optBoolean("isActive", true)

                val isDuplicate = existingRules.any {
                    it.accountId == targetAccountId &&
                    it.amount.compareTo(amount) == 0 &&
                    it.frequency == frequency &&
                    it.note == note
                }

                if (!isDuplicate) {
                    val ruleEntity = RecurringRuleEntity(
                        accountId = targetAccountId,
                        categoryId = targetCategoryId,
                        amount = amount,
                        currency = currency,
                        note = note,
                        frequency = frequency,
                        dayOfMonth = dayOfMonth,
                        startDate = startDate,
                        nextOccurrenceTimestamp = nextTimestamp,
                        isActive = isActive,
                        createdAt = now
                    )
                    recurringRuleDao.insert(ruleEntity)
                    recurringRulesCreated++
                }
            }
        }

        return Result.success(
            ImportResult(
                importedCount = importedCount,
                accountsCreated = accountsCreated,
                categoriesCreated = categoriesCreated,
                personsCreated = personsCreated,
                debtsCreated = debtsCreated,
                recurringRulesCreated = recurringRulesCreated
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
