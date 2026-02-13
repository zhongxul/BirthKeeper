package com.zhongxul.birthkeeper.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabaseFactory
import com.zhongxul.birthkeeper.core.data.repository.ReminderLogRepositoryImpl
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BirthdayReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val personId = intent.getLongExtra(EXTRA_ALARM_PERSON_ID, -1L)
        val personName = intent.getStringExtra(EXTRA_ALARM_PERSON_NAME).orEmpty()
        val relation = intent.getStringExtra(EXTRA_ALARM_PERSON_RELATION).orEmpty()
        val birthday = intent.getStringExtra(EXTRA_ALARM_BIRTHDAY).orEmpty()
        val offsetDay = intent.getIntExtra(EXTRA_ALARM_OFFSET_DAY, 0)
        val logId = intent.getLongExtra(EXTRA_REMINDER_LOG_ID, -1L)

        if (personId <= 0L || logId <= 0L) {
            return
        }

        BirthdayReminderNotifier.notify(
            context = context,
            personId = personId,
            personName = personName,
            relation = relation,
            birthday = birthday,
            offsetDay = offsetDay,
            reminderLogId = logId
        )

        CoroutineScope(Dispatchers.IO).launch {
            val repository = ReminderLogRepositoryImpl(BirthKeeperDatabaseFactory.create(context))
            repository.updateStatus(logId, ReminderLogStatus.SENT)
        }
    }
}
