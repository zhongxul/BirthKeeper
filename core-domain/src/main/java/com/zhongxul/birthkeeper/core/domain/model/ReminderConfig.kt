package com.zhongxul.birthkeeper.core.domain.model

import java.time.LocalTime

data class ReminderConfig(
    val offsets: List<Int> = listOf(7, 3, 1, 0),
    val remindTime: LocalTime = LocalTime.of(9, 0),
    val enabled: Boolean = true
)

