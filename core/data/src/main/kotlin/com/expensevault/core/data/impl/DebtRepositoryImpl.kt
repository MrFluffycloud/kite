package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.DebtRecordDao
import com.expensevault.core.database.dao.SettlementDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.DebtRepository
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.DebtRecord
import com.expensevault.core.model.DebtStatus
import com.expensevault.core.model.Settlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import java.math.BigDecimal

class DebtRepositoryImpl(
    private val debtRecordDao: DebtRecordDao,
    private val settlementDao: SettlementDao
) : DebtRepository {
    override fun getAllDebts(): Flow<List<DebtRecord>> =
        debtRecordDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getDebtsByPerson(personId: Long): Flow<List<DebtRecord>> =
        debtRecordDao.getByPersonId(personId).map { list -> list.map { it.toDomain() } }

    override fun getOpenDebts(): Flow<List<DebtRecord>> =
        debtRecordDao.getOpenDebts().map { list -> list.map { it.toDomain() } }

    override suspend fun getNetBalance(personId: Long): BigDecimal {
        val records = debtRecordDao.getNetBalanceByPerson(personId)
        var balance = BigDecimal.ZERO
        for (record in records) {
            if (record.status == DebtStatus.OPEN) {
                if (record.direction == DebtDirection.THEY_OWE_ME) {
                    balance = balance.add(record.amount)
                } else {
                    balance = balance.subtract(record.amount)
                }
            }
        }
        return balance
    }

    override suspend fun addDebt(debt: DebtRecord): Long =
        debtRecordDao.insert(debt.toEntity())

    override suspend fun updateDebt(debt: DebtRecord) {
        debtRecordDao.update(debt.toEntity())
    }

    override suspend fun settleDebts(personId: Long, debtIds: List<Long>, settlement: Settlement) {
        settlementDao.insert(settlement.toEntity())
        val now = Clock.System.now().toEpochMilliseconds()
        for (id in debtIds) {
            debtRecordDao.markAsSettled(id, now)
        }
    }

    override fun getSettlementsByPerson(personId: Long): Flow<List<Settlement>> =
        settlementDao.getByPersonId(personId).map { list -> list.map { it.toDomain() } }
}
