package com.zhongxul.birthkeeper.core.data.repository

import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabase
import com.zhongxul.birthkeeper.core.data.mapper.toDbValue
import com.zhongxul.birthkeeper.core.data.mapper.toDomain
import com.zhongxul.birthkeeper.core.data.mapper.toEntity
import com.zhongxul.birthkeeper.core.domain.model.ReminderLog
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import com.zhongxul.birthkeeper.core.domain.repository.ReminderLogRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderLogRepositoryImpl(
    private val database: BirthKeeperDatabase
) : ReminderLogRepository {
    override fun observeLogs(): Flow<List<ReminderLog>> {
        return database.reminderLogDao().observeAll().map { logs ->
            logs.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun findByKey(personId: Long, targetDate: LocalDate, offsetDay: Int): ReminderLog? {
        return database.reminderLogDao().findByKey(personId, targetDate.toString(), offsetDay)?.toDomain()
    }

    override suspend fun upsert(log: ReminderLog): Long {
        return database.reminderLogDao().upsert(log.toEntity())
    }

    override suspend fun updateStatus(logId: Long, status: ReminderLogStatus) {
        database.reminderLogDao().updateStatus(logId, status.toDbValue())
    }
}
