package com.zhongxul.birthkeeper.reminder

import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerTest {

    @Test
    fun computeInitialDelay_whenBeforeEight_shouldDelayToTodayEight() {
        val now = LocalDateTime.of(2026, 2, 13, 7, 30)
        val delay = ReminderScheduler.computeInitialDelay(now)
        assertEquals(Duration.ofMinutes(30), delay)
    }

    @Test
    fun computeInitialDelay_whenExactlyEight_shouldDelayOneDay() {
        val now = LocalDateTime.of(2026, 2, 13, 8, 0)
        val delay = ReminderScheduler.computeInitialDelay(now)
        assertEquals(Duration.ofDays(1), delay)
    }

    @Test
    fun computeInitialDelay_whenAfterEight_shouldDelayToNextDayEight() {
        val now = LocalDateTime.of(2026, 2, 13, 9, 15)
        val delay = ReminderScheduler.computeInitialDelay(now)
        assertEquals(Duration.ofHours(22).plusMinutes(45), delay)
    }
}
