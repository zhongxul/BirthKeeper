package com.zhongxul.birthkeeper.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID_BIRTHDAY_REMINDER = "birthday_reminder_channel"
private const val CHANNEL_NAME_BIRTHDAY_REMINDER = "\u751f\u65e5\u63d0\u9192"
private const val UNIQUE_WORK_NAME_DAILY_REMINDER_SCAN = "daily_birthday_reminder_scan"

object ReminderScheduler {
    fun initialize(context: Context) {
        createNotificationChannel(context)
        scheduleDailyScan(context)
    }

    fun reminderChannelId(): String = CHANNEL_ID_BIRTHDAY_REMINDER
    internal fun dailyScanUniqueWorkName(): String = UNIQUE_WORK_NAME_DAILY_REMINDER_SCAN

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID_BIRTHDAY_REMINDER,
            CHANNEL_NAME_BIRTHDAY_REMINDER,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    private fun scheduleDailyScan(context: Context) {
        val initialDelay = computeInitialDelay(LocalDateTime.now())
        val request = PeriodicWorkRequestBuilder<BirthdayReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME_DAILY_REMINDER_SCAN,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    internal fun computeInitialDelay(now: LocalDateTime): Duration {
        val target = now.toLocalDate().atTime(8, 0)
        val nextRun = if (now.isBefore(target)) target else target.plusDays(1)
        return Duration.between(now, nextRun)
    }
}
