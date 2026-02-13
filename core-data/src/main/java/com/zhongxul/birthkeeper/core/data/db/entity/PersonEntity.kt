package com.zhongxul.birthkeeper.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "person",
    indices = [
        Index(value = ["name"]),
        Index(value = ["birthday_solar"])
    ]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "id_number_encrypted")
    val idNumberEncrypted: String?,
    @ColumnInfo(name = "birthday_solar")
    val birthdaySolar: String,
    @ColumnInfo(name = "birthday_lunar")
    val birthdayLunar: String?,
    val gender: Int,
    val relation: String,
    val note: String?,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String?,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

