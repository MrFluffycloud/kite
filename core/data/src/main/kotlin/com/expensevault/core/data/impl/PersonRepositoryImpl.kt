package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.DebtRecordDao
import com.expensevault.core.database.dao.PersonDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.PersonRepository
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.Person
import com.expensevault.core.model.PersonDebtSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class PersonRepositoryImpl(
    private val personDao: PersonDao,
    private val debtRecordDao: DebtRecordDao
) : PersonRepository {
    override fun getPersons(): Flow<List<Person>> =
        personDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getPersonById(id: Long): Person? =
        personDao.getById(id)?.toDomain()

    override suspend fun addPerson(person: Person): Long =
        personDao.insert(person.toEntity())

    override suspend fun updatePerson(person: Person) {
        personDao.update(person.toEntity())
    }

    override suspend fun deletePerson(id: Long) {
        personDao.deleteById(id)
    }

    override fun getPersonDebtSummaries(): Flow<List<PersonDebtSummary>> {
        return combine(personDao.getAll(), debtRecordDao.getOpenDebts()) { persons, openDebts ->
            persons.map { personEntity ->
                val person = personEntity.toDomain()
                val personDebts = openDebts.filter { it.personId == person.id }
                var balance = BigDecimal.ZERO
                var currency = "INR"
                for (debt in personDebts) {
                    currency = debt.currency
                    if (debt.direction == DebtDirection.THEY_OWE_ME) {
                        balance = balance.add(debt.amount)
                    } else {
                        balance = balance.subtract(debt.amount)
                    }
                }
                PersonDebtSummary(
                    person = person,
                    netBalance = balance,
                    currency = currency,
                    openDebtsCount = personDebts.size
                )
            }
        }
    }
}
