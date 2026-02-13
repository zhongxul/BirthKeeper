package com.zhongxul.birthkeeper.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhongxul.birthkeeper.core.data.db.entity.ReminderLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderLogDao {
    @Query("SELECT * FROM reminder_log ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ReminderLogEntity>>

    @Query(
        "SELECT * FROM reminder_log " +
            "WHERE person_id = :personId AND target_date = :targetDate AND offset_day = :offsetDay " +
            "LIMIT 1"
    )
    suspend fun findByKey(personId: Long, targetDate: String, offsetDay: Int): ReminderLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: ReminderLogEntity): Long

    @Query("UPDATE reminder_log SET status = :status WHERE id = :logId")
    suspend fun updateStatus(logId: Long, status: Int)

    @Query("DELETE FROM reminder_log")
    suspend fun deleteAll()
}
