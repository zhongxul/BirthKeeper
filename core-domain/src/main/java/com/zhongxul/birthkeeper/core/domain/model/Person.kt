package com.zhongxul.birthkeeper.core.domain.model

import java.time.LocalDate

data class Person(
    val id: Long = 0L,
    val name: String,
    val idNumber: String?,
    val birthdaySolar: LocalDate,
    val birthdayLunar: String?,
    val gender: Gender = Gender.UNKNOWN,
    val relation: String,
    val note: String?,
    val avatarUri: String?,
    val reminderConfig: ReminderConfig = ReminderConfig(),
    val createdAt: Long,
    val updatedAt: Long
)

