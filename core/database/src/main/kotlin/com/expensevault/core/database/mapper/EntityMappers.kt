package com.expensevault.core.database.mapper

import com.expensevault.core.database.entity.*
import com.expensevault.core.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)
fun Instant.toEpochMillis(): Long = this.toEpochMilliseconds()

fun String.toLocalDate(): LocalDate = LocalDate.parse(this)
fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays(this.toInt())
fun LocalDate.toEpochDaysLong(): Long = this.toEpochDays().toLong()

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = type,
    defaultCurrency = defaultCurrency,
    initialBalance = initialBalance,
    currentBalance = currentBalance,
    iconName = iconName,
    colorHex = colorHex,
    isArchived = isArchived,
    sortOrder = sortOrder,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant()
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type,
    defaultCurrency = defaultCurrency,
    initialBalance = initialBalance,
    currentBalance = currentBalance,
    iconName = iconName,
    colorHex = colorHex,
    isArchived = isArchived,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMillis(),
    updatedAt = updatedAt.toEpochMillis()
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    parentId = parentId,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault,
    isArchived = isArchived,
    sortOrder = sortOrder,
    createdAt = createdAt.toInstant()
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    parentId = parentId,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault,
    isArchived = isArchived,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMillis()
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    type = type,
    originalAmount = originalAmount,
    originalCurrency = originalCurrency,
    exchangeRate = exchangeRate,
    baseAmount = baseAmount,
    note = note,
    merchant = merchant,
    locationLat = locationLat,
    locationLng = locationLng,
    locationName = locationName,
    transactionDate = transactionDate.toLocalDate(),
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    recurringRuleId = recurringRuleId,
    source = source,
    isEstimatedRate = isEstimatedRate
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    type = type,
    originalAmount = originalAmount,
    originalCurrency = originalCurrency,
    exchangeRate = exchangeRate,
    baseAmount = baseAmount,
    note = note,
    merchant = merchant,
    locationLat = locationLat,
    locationLng = locationLng,
    locationName = locationName,
    transactionDate = transactionDate.toEpochDaysLong(),
    createdAt = createdAt.toEpochMillis(),
    updatedAt = updatedAt.toEpochMillis(),
    recurringRuleId = recurringRuleId,
    deduplicationKey = null,
    source = source,
    isEstimatedRate = isEstimatedRate
)

fun ReceiptPhotoEntity.toDomain() = ReceiptPhoto(
    id = id,
    transactionId = transactionId,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    capturedAt = capturedAt.toInstant()
)

fun ReceiptPhoto.toEntity() = ReceiptPhotoEntity(
    id = id,
    transactionId = transactionId,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    capturedAt = capturedAt.toEpochMillis()
)

fun PersonEntity.toDomain() = Person(
    id = id,
    name = name,
    phone = phone,
    avatarPath = avatarPath,
    createdAt = createdAt.toInstant()
)

fun Person.toEntity() = PersonEntity(
    id = id,
    name = name,
    phone = phone,
    avatarPath = avatarPath,
    createdAt = createdAt.toEpochMillis()
)

fun DebtRecordEntity.toDomain() = DebtRecord(
    id = id,
    personId = personId,
    transactionId = transactionId,
    amount = amount,
    currency = currency,
    direction = direction,
    status = status,
    splitMethod = splitMethod,
    note = note,
    createdAt = createdAt.toInstant(),
    settledAt = settledAt?.toInstant()
)

fun DebtRecord.toEntity() = DebtRecordEntity(
    id = id,
    personId = personId,
    transactionId = transactionId,
    amount = amount,
    currency = currency,
    direction = direction,
    status = status,
    splitMethod = splitMethod,
    note = note,
    createdAt = createdAt.toEpochMillis(),
    settledAt = settledAt?.toEpochMillis()
)

fun SettlementEntity.toDomain() = Settlement(
    id = id,
    personId = personId,
    settledAmount = settledAmount,
    currency = currency,
    note = note,
    settledAt = settledAt.toInstant(),
    debtRecordIds = debtRecordIds
)

fun Settlement.toEntity() = SettlementEntity(
    id = id,
    personId = personId,
    settledAmount = settledAmount,
    currency = currency,
    note = note,
    settledAt = settledAt.toEpochMillis(),
    debtRecordIds = debtRecordIds
)

fun RecurringRuleEntity.toDomain() = RecurringRule(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    currency = currency,
    note = note,
    frequency = frequency,
    customIntervalDays = customIntervalDays,
    dayOfWeek = dayOfWeek,
    dayOfMonth = dayOfMonth,
    startDate = startDate.toLocalDate(),
    endDate = endDate?.toLocalDate(),
    nextOccurrence = nextOccurrenceTimestamp.toLocalDate(),
    requireConfirmation = requireConfirmation,
    isActive = isActive,
    createdAt = createdAt.toInstant()
)

fun RecurringRule.toEntity() = RecurringRuleEntity(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    currency = currency,
    note = note,
    frequency = frequency,
    customIntervalDays = customIntervalDays,
    dayOfWeek = dayOfWeek,
    dayOfMonth = dayOfMonth,
    startDate = startDate.toEpochDaysLong(),
    endDate = endDate?.toEpochDaysLong(),
    nextOccurrenceTimestamp = nextOccurrence.toEpochDaysLong(),
    requireConfirmation = requireConfirmation,
    isActive = isActive,
    createdAt = createdAt.toEpochMillis()
)

fun BudgetCapEntity.toDomain() = BudgetCap(
    id = id,
    categoryId = categoryId,
    limitAmount = limitAmount,
    currency = currency,
    periodType = periodType,
    warningThreshold = warningThreshold,
    isActive = isActive,
    createdAt = createdAt.toInstant()
)

fun BudgetCap.toEntity() = BudgetCapEntity(
    id = id,
    categoryId = categoryId,
    limitAmount = limitAmount,
    currency = currency,
    periodType = periodType,
    warningThreshold = warningThreshold,
    isActive = isActive,
    createdAt = createdAt.toEpochMillis()
)

fun ExchangeRateEntity.toDomain() = ExchangeRate(
    baseCurrency = baseCurrency,
    targetCurrency = targetCurrency,
    rate = rate,
    rateDate = rateDate.toLocalDate(),
    fetchedAt = fetchedAt.toInstant()
)

fun ExchangeRate.toEntity() = ExchangeRateEntity(
    baseCurrency = baseCurrency,
    targetCurrency = targetCurrency,
    rate = rate,
    rateDate = rateDate.toString(),
    fetchedAt = fetchedAt.toEpochMillis()
)

fun com.expensevault.core.database.entity.VaultExpenseEntity.toDomain() = com.expensevault.core.model.VaultExpense(
    id = id,
    amount = amount,
    currency = currency,
    title = title,
    note = note,
    category = category,
    date = date.toLocalDate(),
    isSharedWithPartner = isSharedWithPartner,
    partnerShare = partnerShare,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant()
)

fun com.expensevault.core.model.VaultExpense.toEntity() = com.expensevault.core.database.entity.VaultExpenseEntity(
    id = id,
    amount = amount,
    currency = currency,
    title = title,
    note = note,
    category = category,
    date = date.toString(),
    isSharedWithPartner = isSharedWithPartner,
    partnerShare = partnerShare,
    createdAt = createdAt.toEpochMillis(),
    updatedAt = updatedAt.toEpochMillis()
)
