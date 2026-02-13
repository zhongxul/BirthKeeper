package com.zhongxul.birthkeeper.reminder

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerIntegrationTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun initialize_shouldEnqueueUniqueDailyScanWorkAndCreateChannel() {
        ReminderScheduler.initialize(context)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(ReminderScheduler.dailyScanUniqueWorkName())
            .get()
        assertEquals(1, workInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, workInfos.first().state)
        assertTrue(workInfos.first().tags.contains(BirthdayReminderWorker::class.java.name))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(ReminderScheduler.reminderChannelId())
            assertNotNull(channel)
            assertEquals(ReminderScheduler.reminderChannelId(), channel.id)
        }
    }
}
