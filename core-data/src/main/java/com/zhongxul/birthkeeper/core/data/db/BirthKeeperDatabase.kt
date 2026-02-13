package com.zhongxul.birthkeeper.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zhongxul.birthkeeper.core.data.db.dao.PersonDao
import com.zhongxul.birthkeeper.core.data.db.dao.ReminderConfigDao
import com.zhongxul.birthkeeper.core.data.db.dao.ReminderLogDao
import com.zhongxul.birthkeeper.core.data.db.entity.PersonEntity
import com.zhongxul.birthkeeper.core.data.db.entity.ReminderConfigEntity
import com.zhongxul.birthkeeper.core.data.db.entity.ReminderLogEntity

@Database(
    entities = [PersonEntity::class, ReminderConfigEntity::class, ReminderLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class BirthKeeperDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun reminderConfigDao(): ReminderConfigDao
    abstract fun reminderLogDao(): ReminderLogDao
}
