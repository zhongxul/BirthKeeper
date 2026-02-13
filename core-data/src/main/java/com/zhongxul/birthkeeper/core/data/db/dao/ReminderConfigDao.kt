package com.zhongxul.birthkeeper.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhongxul.birthkeeper.core.data.db.entity.ReminderConfigEntity

@Dao
interface ReminderConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: ReminderConfigEntity): Long

    @Query("DELETE FROM reminder_config WHERE person_id = :personId")
    suspend fun deleteByPersonId(personId: Long)

    @Query("DELETE FROM reminder_config")
    suspend fun deleteAll()
}
