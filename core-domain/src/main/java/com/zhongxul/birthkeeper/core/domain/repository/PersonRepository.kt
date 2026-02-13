package com.zhongxul.birthkeeper.core.domain.repository

import com.zhongxul.birthkeeper.core.domain.model.Person
import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun observePeople(): Flow<List<Person>>
    suspend fun getPersonById(id: Long): Person?
    suspend fun upsert(person: Person): Long
    suspend fun softDelete(id: Long)
}

