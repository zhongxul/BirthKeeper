package com.zhongxul.birthkeeper.core.data.mapper

import com.zhongxul.birthkeeper.core.data.db.entity.ReminderLogEntity
import com.zhongxul.birthkeeper.core.domain.model.ReminderLog
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import java.time.LocalDate

fun ReminderLogEntity.toDomain(): ReminderLog {
    return ReminderLog(
        id = id,
        personId = personId,
        targetDate = LocalDate.parse(targetDate),
        offsetDay = offsetDay,
        status = status.toReminderStatus(),
        createdAt = createdAt
    )
}

fun ReminderLog.toEntity(): ReminderLogEntity {
    return ReminderLogEntity(
        id = id,
        personId = personId,
        targetDate = targetDate.toString(),
        offsetDay = offsetDay,
        status = status.toDbValue(),
        createdAt = createdAt
    )
}

fun ReminderLogStatus.toDbValue(): Int {
    return when (this) {
        ReminderLogStatus.PLANNED -> 0
        ReminderLogStatus.SENT -> 1
        ReminderLogStatus.CLICKED -> 2
        ReminderLogStatus.DONE -> 3
    }
}

private fun Int.toReminderStatus(): ReminderLogStatus {
    return when (this) {
        1 -> ReminderLogStatus.SENT
        2 -> ReminderLogStatus.CLICKED
        3 -> ReminderLogStatus.DONE
        else -> ReminderLogStatus.PLANNED
    }
}
