package com.zhongxul.birthkeeper.core.data.db

import android.content.Context
import androidx.room.Room

object BirthKeeperDatabaseFactory {
    fun create(context: Context): BirthKeeperDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            BirthKeeperDatabase::class.java,
            "birth_keeper.db"
        ).build()
    }
}

