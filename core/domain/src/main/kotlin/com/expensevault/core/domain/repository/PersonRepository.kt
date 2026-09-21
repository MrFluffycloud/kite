package com.expensevault.core.domain.repository

import com.expensevault.core.model.Person
import com.expensevault.core.model.PersonDebtSummary
import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun getPersons(): Flow<List<Person>>
    suspend fun getPersonById(id: Long): Person?
    suspend fun addPerson(person: Person): Long
    suspend fun updatePerson(person: Person)
    suspend fun deletePerson(id: Long)
    fun getPersonDebtSummaries(): Flow<List<PersonDebtSummary>>
}
