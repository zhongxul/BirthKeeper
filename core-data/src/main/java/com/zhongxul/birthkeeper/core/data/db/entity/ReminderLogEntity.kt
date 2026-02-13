package com.zhongxul.birthkeeper.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_log",
    indices = [Index(value = ["target_date", "status"])]
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "person_id")
    val personId: Long,
    @ColumnInfo(name = "target_date")
    val targetDate: String,
    @ColumnInfo(name = "offset_day")
    val offsetDay: Int,
    val status: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

