package com.zhongxul.birthkeeper

import android.app.Application
import com.zhongxul.birthkeeper.core.common.util.IdNumberCrypto
import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabase
import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabaseFactory
import com.zhongxul.birthkeeper.core.data.repository.BackupRepositoryImpl
import com.zhongxul.birthkeeper.core.data.repository.PersonRepositoryImpl
import com.zhongxul.birthkeeper.core.data.repository.ReminderLogRepositoryImpl
import com.zhongxul.birthkeeper.core.domain.repository.BackupRepository
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository
import com.zhongxul.birthkeeper.core.domain.repository.ReminderLogRepository
import com.zhongxul.birthkeeper.reminder.ReminderScheduler

class BirthKeeperApplication : Application() {
    val database: BirthKeeperDatabase by lazy {
        BirthKeeperDatabaseFactory.create(this)
    }

    val personRepository: PersonRepository by lazy {
        PersonRepositoryImpl(
            database = database,
            encryptIdNumber = { value -> IdNumberCrypto.encrypt(value) },
            decryptIdNumber = { value -> IdNumberCrypto.decrypt(value) }
        )
    }

    val reminderLogRepository: ReminderLogRepository by lazy {
        ReminderLogRepositoryImpl(database)
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(database)
    }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.initialize(this)
    }
}
