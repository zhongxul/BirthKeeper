package com.zhongxul.birthkeeper.core.data.mapper

import com.zhongxul.birthkeeper.core.data.db.entity.PersonEntity
import com.zhongxul.birthkeeper.core.data.db.entity.PersonWithReminder
import com.zhongxul.birthkeeper.core.data.db.entity.ReminderConfigEntity
import com.zhongxul.birthkeeper.core.domain.model.Gender
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderConfig
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun PersonWithReminder.toDomain(): Person {
    return Person(
        id = person.id,
        name = person.name,
        idNumber = person.idNumberEncrypted,
        birthdaySolar = LocalDate.parse(person.birthdaySolar, dateFormatter),
        birthdayLunar = person.birthdayLunar,
        gender = person.gender.toGender(),
        relation = person.relation,
        note = person.note,
        avatarUri = person.avatarUri,
        reminderConfig = reminderConfig?.toDomain() ?: ReminderConfig(),
        createdAt = person.createdAt,
        updatedAt = person.updatedAt
    )
}

fun Person.toEntity(createdAt: Long = this.createdAt, updatedAt: Long = this.updatedAt): PersonEntity {
    return PersonEntity(
        id = id,
        name = name,
        // 第 1 周先保留明文占位，后续在安全模块中替换为真实加密数据。
        idNumberEncrypted = idNumber,
        birthdaySolar = birthdaySolar.format(dateFormatter),
        birthdayLunar = birthdayLunar,
        gender = gender.toDbValue(),
        relation = relation,
        note = note,
        avatarUri = avatarUri,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ReminderConfig.toEntity(personId: Long): ReminderConfigEntity {
    return ReminderConfigEntity(
        personId = personId,
        offsetsJson = offsets.joinToString(prefix = "[", postfix = "]"),
        remindTime = remindTime.format(timeFormatter),
        enabled = if (enabled) 1 else 0
    )
}

private fun ReminderConfigEntity.toDomain(): ReminderConfig {
    return ReminderConfig(
        offsets = offsetsJson
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .mapNotNull { item -> item.trim().toIntOrNull() }
            .ifEmpty { listOf(7, 3, 1, 0) },
        remindTime = LocalTime.parse(remindTime, timeFormatter),
        enabled = enabled == 1
    )
}

private fun Int.toGender(): Gender {
    return when (this) {
        1 -> Gender.MALE
        2 -> Gender.FEMALE
        else -> Gender.UNKNOWN
    }
}

private fun Gender.toDbValue(): Int {
    return when (this) {
        Gender.MALE -> 1
        Gender.FEMALE -> 2
        Gender.UNKNOWN -> 0
    }
}

