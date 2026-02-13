package com.zhongxul.birthkeeper.core.data.repository

import androidx.room.withTransaction
import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabase
import com.zhongxul.birthkeeper.core.data.mapper.toDomain
import com.zhongxul.birthkeeper.core.data.mapper.toEntity
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonRepositoryImpl(
    private val database: BirthKeeperDatabase,
    private val encryptIdNumber: (String) -> String = { value -> value },
    private val decryptIdNumber: (String) -> String = { value -> value }
) : PersonRepository {

    override fun observePeople(): Flow<List<Person>> {
        return database.personDao().observeAllActive().map { people ->
            people.map { item ->
                val domain = item.toDomain()
                domain.copy(idNumber = domain.idNumber?.let { decryptIdNumber(it) })
            }
        }
    }

    override suspend fun getPersonById(id: Long): Person? {
        return database.personDao().findById(id)?.toDomain()?.let { person ->
            person.copy(
                idNumber = person.idNumber?.let { decryptIdNumber(it) }
            )
        }
    }

    override suspend fun upsert(person: Person): Long {
        val encryptedPerson = person.copy(
            idNumber = person.idNumber?.let { encryptIdNumber(it) }
        )
        val now = System.currentTimeMillis()
        val createdAt = if (person.id == 0L) now else person.createdAt
        return database.withTransaction {
            val savedId = database.personDao().upsertPerson(
                encryptedPerson.toEntity(createdAt = createdAt, updatedAt = now)
            )
            val personId = if (savedId > 0L) savedId else person.id
            database.reminderConfigDao().upsert(person.reminderConfig.toEntity(personId))
            personId
        }
    }

    override suspend fun softDelete(id: Long) {
        database.withTransaction {
            database.personDao().softDelete(id, System.currentTimeMillis())
            database.reminderConfigDao().deleteByPersonId(id)
        }
    }
}
