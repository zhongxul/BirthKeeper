package com.zhongxul.birthkeeper.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_config",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["person_id"], unique = true)]
)
data class ReminderConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "person_id")
    val personId: Long,
    @ColumnInfo(name = "offsets_json")
    val offsetsJson: String,
    @ColumnInfo(name = "remind_time")
    val remindTime: String,
    val enabled: Int
)

