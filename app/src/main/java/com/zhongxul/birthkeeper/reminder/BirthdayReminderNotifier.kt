package com.zhongxul.birthkeeper.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zhongxul.birthkeeper.MainActivity
import com.zhongxul.birthkeeper.R

object BirthdayReminderNotifier {
    fun notify(
        context: Context,
        personId: Long,
        personName: String,
        relation: String,
        birthday: String,
        offsetDay: Int,
        reminderLogId: Long
    ) {
        val title = if (offsetDay == 0) {
            "\u4eca\u5929\u662f $personName \u7684\u751f\u65e5"
        } else {
            "$personName \u5c06\u5728 $offsetDay \u5929\u540e\u751f\u65e5"
        }
        val content = "\u5173\u7cfb\uff1a$relation\uff0c\u751f\u65e5\uff1a$birthday"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PERSON_ID, personId)
            putExtra(EXTRA_REMINDER_LOG_ID, reminderLogId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderLogId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.reminderChannelId())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(reminderLogId.toInt(), notification)
    }
}
