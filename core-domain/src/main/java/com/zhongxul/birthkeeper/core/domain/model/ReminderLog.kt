package com.zhongxul.birthkeeper.core.domain.model

import java.time.LocalDate

data class ReminderLog(
    val id: Long = 0L,
    val personId: Long,
    val targetDate: LocalDate,
    val offsetDay: Int,
    val status: ReminderLogStatus,
    val createdAt: Long
)

enum class ReminderLogStatus {
    PLANNED,
    SENT,
    CLICKED,
    DONE
}
