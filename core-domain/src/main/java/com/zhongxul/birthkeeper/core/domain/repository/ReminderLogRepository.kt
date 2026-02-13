package com.zhongxul.birthkeeper.core.domain.repository

import com.zhongxul.birthkeeper.core.domain.model.ReminderLog
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ReminderLogRepository {
    fun observeLogs(): Flow<List<ReminderLog>>
    suspend fun findByKey(personId: Long, targetDate: LocalDate, offsetDay: Int): ReminderLog?
    suspend fun upsert(log: ReminderLog): Long
    suspend fun updateStatus(logId: Long, status: ReminderLogStatus)
}
