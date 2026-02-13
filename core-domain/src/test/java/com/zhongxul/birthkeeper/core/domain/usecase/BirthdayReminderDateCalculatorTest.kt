package com.zhongxul.birthkeeper.core.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BirthdayReminderDateCalculatorTest {

    @Test
    fun `生日未到时应返回当年生日`() {
        val birthday = LocalDate.of(1990, 12, 1)
        val today = LocalDate.of(2026, 2, 13)

        val next = BirthdayReminderDateCalculator.nextBirthday(birthday, today)
        val days = BirthdayReminderDateCalculator.daysUntilBirthday(birthday, today)

        assertEquals(LocalDate.of(2026, 12, 1), next)
        assertEquals(291, days)
    }

    @Test
    fun `生日已过时应返回下一年生日`() {
        val birthday = LocalDate.of(1990, 1, 1)
        val today = LocalDate.of(2026, 2, 13)

        val next = BirthdayReminderDateCalculator.nextBirthday(birthday, today)

        assertEquals(LocalDate.of(2027, 1, 1), next)
    }

    @Test
    fun `闰日生日在非闰年应落到2月最后一天`() {
        val birthday = LocalDate.of(2000, 2, 29)
        val today = LocalDate.of(2026, 2, 10)

        val next = BirthdayReminderDateCalculator.nextBirthday(birthday, today)
        val days = BirthdayReminderDateCalculator.daysUntilBirthday(birthday, today)

        assertEquals(LocalDate.of(2026, 2, 28), next)
        assertEquals(18, days)
    }
}
