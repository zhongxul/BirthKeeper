package com.zhongxul.birthkeeper.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zhongxul.birthkeeper.core.data.db.entity.PersonEntity
import com.zhongxul.birthkeeper.core.data.db.entity.PersonWithReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Transaction
    @Query("SELECT * FROM person WHERE is_deleted = 0 ORDER BY birthday_solar ASC")
    fun observeAllActive(): Flow<List<PersonWithReminder>>

    @Transaction
    @Query("SELECT * FROM person WHERE is_deleted = 0 ORDER BY id ASC")
    suspend fun listAllActive(): List<PersonWithReminder>

    @Transaction
    @Query("SELECT * FROM person WHERE is_deleted = 0 AND id = :id LIMIT 1")
    suspend fun findById(id: Long): PersonWithReminder?

    @Query("SELECT * FROM person WHERE id = :id LIMIT 1")
    suspend fun findPersonEntityById(id: Long): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPerson(person: PersonEntity): Long

    @Query("UPDATE person SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("DELETE FROM person")
    suspend fun deleteAll()
}
