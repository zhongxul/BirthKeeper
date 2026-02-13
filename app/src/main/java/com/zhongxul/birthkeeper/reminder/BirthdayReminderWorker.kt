package com.zhongxul.birthkeeper.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zhongxul.birthkeeper.core.data.db.BirthKeeperDatabaseFactory
import com.zhongxul.birthkeeper.core.data.repository.PersonRepositoryImpl
import com.zhongxul.birthkeeper.core.data.repository.ReminderLogRepositoryImpl
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderLog
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import com.zhongxul.birthkeeper.core.domain.usecase.BirthdayReminderDateCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

class BirthdayReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database by lazy { BirthKeeperDatabaseFactory.create(applicationContext) }
    private val personRepository by lazy { PersonRepositoryImpl(database) }
    private val reminderLogRepository by lazy { ReminderLogRepositoryImpl(database) }

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val people = personRepository.observePeople().first()
        people.forEach { person ->
            val config = person.reminderConfig
            if (!config.enabled) {
                return@forEach
            }
            val targetBirthday = BirthdayReminderDateCalculator.nextBirthday(person.birthdaySolar, today)
            val daysUntilBirthday = BirthdayReminderDateCalculator.daysUntilBirthday(person.birthdaySolar, today)
            if (!config.offsets.contains(daysUntilBirthday)) {
                return@forEach
            }
            handleReminderForOffset(
                person = person,
                targetDate = targetBirthday,
                offsetDay = daysUntilBirthday,
                remindTime = config.remindTime,
                now = now
            )
        }
        return Result.success()
    }

    private suspend fun handleReminderForOffset(
        person: Person,
        targetDate: LocalDate,
        offsetDay: Int,
        remindTime: LocalTime,
        now: LocalDateTime
    ) {
        val existingLog = ensureReminderLog(person.id, targetDate, offsetDay)
        if (existingLog.status == ReminderLogStatus.DONE) {
            return
        }
        if (!hasNotificationPermission()) {
            return
        }

        val shouldExactSchedule = offsetDay == 0 && now.toLocalDate() == targetDate && now.toLocalTime().isBefore(remindTime)
        if (shouldExactSchedule && canScheduleExactAlarm()) {
            scheduleExactAlarm(person, existingLog.id, targetDate, remindTime, offsetDay)
            return
        }

        if (existingLog.status == ReminderLogStatus.SENT || existingLog.status == ReminderLogStatus.CLICKED) {
            return
        }

        BirthdayReminderNotifier.notify(
            context = applicationContext,
            personId = person.id,
            personName = person.name,
            relation = person.relation,
            birthday = targetDate.toString(),
            offsetDay = offsetDay,
            reminderLogId = existingLog.id
        )
        reminderLogRepository.updateStatus(existingLog.id, ReminderLogStatus.SENT)
    }

    private suspend fun ensureReminderLog(personId: Long, targetDate: LocalDate, offsetDay: Int): ReminderLog {
        val existing = reminderLogRepository.findByKey(personId, targetDate, offsetDay)
        if (existing != null) {
            return existing
        }
        val now = System.currentTimeMillis()
        val newLog = ReminderLog(
            personId = personId,
            targetDate = targetDate,
            offsetDay = offsetDay,
            status = ReminderLogStatus.PLANNED,
            createdAt = now
        )
        val newId = reminderLogRepository.upsert(newLog)
        return reminderLogRepository.findByKey(personId, targetDate, offsetDay)
            ?: newLog.copy(id = if (newId > 0L) newId else 0L)
    }

    private fun scheduleExactAlarm(
        person: Person,
        reminderLogId: Long,
        targetDate: LocalDate,
        remindTime: LocalTime,
        offsetDay: Int
    ) {
        val triggerMillis = targetDate
            .atTime(remindTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) {
            return
        }
        val alarmIntent = Intent(applicationContext, BirthdayReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_PERSON_ID, person.id)
            putExtra(EXTRA_ALARM_PERSON_NAME, person.name)
            putExtra(EXTRA_ALARM_PERSON_RELATION, person.relation)
            putExtra(EXTRA_ALARM_BIRTHDAY, targetDate.toString())
            putExtra(EXTRA_ALARM_OFFSET_DAY, offsetDay)
            putExtra(EXTRA_REMINDER_LOG_ID, reminderLogId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            reminderLogId.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        val permission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return permission == PackageManager.PERMISSION_GRANTED
    }

    private fun canScheduleExactAlarm(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

}
