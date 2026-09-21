package com.expensevault.core.data.usecase

import com.expensevault.core.database.dao.AccountDao
import com.expensevault.core.database.dao.CategoryDao
import com.expensevault.core.database.dao.TransactionDao
import com.expensevault.core.database.dao.DebtRecordDao
import com.expensevault.core.database.dao.PersonDao
import com.expensevault.core.database.dao.RecurringRuleDao
import com.expensevault.core.domain.usecase.ExportDataUseCase
import com.expensevault.core.domain.usecase.ExportFormat
import com.expensevault.core.domain.usecase.ExportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class ExportDataUseCaseImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val personDao: PersonDao,
    private val debtRecordDao: DebtRecordDao,
    private val recurringRuleDao: RecurringRuleDao
) : ExportDataUseCase {

    override suspend fun export(format: ExportFormat): ExportResult = withContext(Dispatchers.IO) {
        val transactions = transactionDao.getAll().firstOrNull() ?: emptyList()
        val accounts = accountDao.getAll().firstOrNull() ?: emptyList()
        val categories = categoryDao.getAll().firstOrNull() ?: emptyList()
        val persons = personDao.getAll().firstOrNull() ?: emptyList()
        val debts = debtRecordDao.getAll().firstOrNull() ?: emptyList()
        val recurringRules = recurringRuleDao.getAll().firstOrNull() ?: emptyList()

        val accountMap = accounts.associate { it.id to it.name }
        val categoryMap = categories.associate { it.id to it.name }

        val timestamp = Clock.System.now().toEpochMilliseconds()

        when (format) {
            ExportFormat.CSV -> {
                val csvBuilder = StringBuilder()
                // CSV Header
                csvBuilder.append("ID,Date,Type,Amount,Currency,BaseAmount,Account,Category,Merchant,Note,Source\n")

                for (t in transactions) {
                    val dateStr = try {
                        LocalDate.fromEpochDays(t.transactionDate.toInt()).toString()
                    } catch (e: Exception) {
                        t.transactionDate.toString()
                    }
                    val accountName = accountMap[t.accountId] ?: "Account_${t.accountId}"
                    val categoryName = categoryMap[t.categoryId] ?: "Uncategorized"

                    csvBuilder.append(t.id).append(",")
                    csvBuilder.append(escapeCsv(dateStr)).append(",")
                    csvBuilder.append(escapeCsv(t.type.name)).append(",")
                    csvBuilder.append(t.originalAmount.toPlainString()).append(",")
                    csvBuilder.append(escapeCsv(t.originalCurrency)).append(",")
                    csvBuilder.append(t.baseAmount.toPlainString()).append(",")
                    csvBuilder.append(escapeCsv(accountName)).append(",")
                    csvBuilder.append(escapeCsv(categoryName)).append(",")
                    csvBuilder.append(escapeCsv(t.merchant.orEmpty())).append(",")
                    csvBuilder.append(escapeCsv(t.note.orEmpty())).append(",")
                    csvBuilder.append(escapeCsv(t.source.name)).append("\n")
                }

                ExportResult(
                    fileName = "expense_vault_export_$timestamp.csv",
                    mimeType = "text/csv",
                    content = csvBuilder.toString()
                )
            }

            ExportFormat.JSON -> {
                val jsonBuilder = StringBuilder()
                jsonBuilder.append("{\n")
                jsonBuilder.append("  \"exportedAt\": ").append(timestamp).append(",\n")
                jsonBuilder.append("  \"accounts\": [\n")
                accounts.forEachIndexed { index, a ->
                    jsonBuilder.append("    {\"id\": ${a.id}, \"name\": \"${escapeJson(a.name)}\", \"currency\": \"${a.defaultCurrency}\", \"balance\": \"${a.currentBalance}\"}")
                    if (index < accounts.size - 1) jsonBuilder.append(",")
                    jsonBuilder.append("\n")
                }
                jsonBuilder.append("  ],\n")
                jsonBuilder.append("  \"categories\": [\n")
                categories.forEachIndexed { index, c ->
                    jsonBuilder.append("    {\"id\": ${c.id}, \"name\": \"${escapeJson(c.name)}\", \"parentId\": ${c.parentId}}")
                    if (index < categories.size - 1) jsonBuilder.append(",")
                    jsonBuilder.append("\n")
                }
                jsonBuilder.append("  ],\n")
                jsonBuilder.append("  \"persons\": [\n")
                persons.forEachIndexed { index, p ->
                    jsonBuilder.append("    {\"id\": ${p.id}, \"name\": \"${escapeJson(p.name)}\", \"phone\": \"${escapeJson(p.phone.orEmpty())}\"}")
                    if (index < persons.size - 1) jsonBuilder.append(",")
                    jsonBuilder.append("\n")
                }
                jsonBuilder.append("  ],\n")
                jsonBuilder.append("  \"debts\": [\n")
                debts.forEachIndexed { index, d ->
                    jsonBuilder.append("    {\"id\": ${d.id}, \"personId\": ${d.personId}, \"transactionId\": ${d.transactionId}, \"amount\": \"${d.amount}\", \"currency\": \"${d.currency}\", \"direction\": \"${d.direction.name}\", \"status\": \"${d.status.name}\", \"splitMethod\": ${d.splitMethod?.let { "\"${it.name}\"" } ?: "null"}, \"note\": \"${escapeJson(d.note.orEmpty())}\", \"createdAt\": ${d.createdAt}, \"settledAt\": ${d.settledAt}}")
                    if (index < debts.size - 1) jsonBuilder.append(",")
                    jsonBuilder.append("\n")
                }
                jsonBuilder.append("  ],\n")
                jsonBuilder.append("  \"recurringRules\": [\n")
                recurringRules.forEachIndexed { index, r ->
                    jsonBuilder.append("    {\"id\": ${r.id}, \"accountId\": ${r.accountId}, \"categoryId\": ${r.categoryId}, \"amount\": \"${r.amount}\", \"currency\": \"${r.currency}\", \"frequency\": \"${r.frequency.name}\", \"dayOfMonth\": ${r.dayOfMonth}, \"note\": \"${escapeJson(r.note.orEmpty())}\", \"startDate\": ${r.startDate}, \"nextOccurrenceTimestamp\": ${r.nextOccurrenceTimestamp}, \"isActive\": ${r.isActive}}")
                    if (index < recurringRules.size - 1) jsonBuilder.append(",")
                    jsonBuilder.append("\n")
                }
                jsonBuilder.append("  ],\n")
                jsonBuilder.append("  \"transactions\": [\n")
                transactions.forEachIndexed { index, t ->
                    val dateStr = try {
                        LocalDate.fromEpochDays(t.transactionDate.toInt()).toString()
                    } catch (e: Exception) {
                        t.transactionDate.toString()
                    }
                    jsonBuilder.append("    {")
                    jsonBuilder.append("\"id\": ${t.id}, ")
                    jsonBuilder.append("\"date\": \"$dateStr\", ")
                    jsonBuilder.append("\"type\": \"${t.type.name}\", ")
                    jsonBuilder.append("\"originalAmount\": \"${t.originalAmount}\", ")
                    jsonBuilder.append("\"originalCurrency\": \"${t.originalCurrency}\", ")
                    jsonBuilder.append("\"baseAmount\": \"${t.baseAmount}\", ")
                    jsonBuilder.append("\"accountId\": ${t.accountId}, ")
                    jsonBuilder.append("\"categoryId\": ${t.categoryId}, ")
                    jsonBuilder.append("\"merchant\": \"${escapeJson(t.merchant.orEmpty())}\", ")
                    jsonBuilder.append("\"note\": \"${escapeJson(t.note.orEmpty())}\", ")
                    jsonBuilder.append("\"source\": \"${t.source.name}\"")
                    jsonBuilder.append("}")
                    if (index < transactions.size - 1) jsonBuilder.append(",")
                    jsonBuilder.append("\n")
                }
                jsonBuilder.append("  ]\n")
                jsonBuilder.append("}")

                ExportResult(
                    fileName = "expense_vault_backup_$timestamp.json",
                    mimeType = "application/json",
                    content = jsonBuilder.toString()
                )
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
