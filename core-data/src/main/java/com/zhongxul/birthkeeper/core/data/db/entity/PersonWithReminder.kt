package com.zhongxul.birthkeeper.core.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PersonWithReminder(
    @Embedded
    val person: PersonEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val reminderConfig: ReminderConfigEntity?
)

