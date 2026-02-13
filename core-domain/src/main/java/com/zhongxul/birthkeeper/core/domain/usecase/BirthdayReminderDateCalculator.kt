package com.zhongxul.birthkeeper.core.domain.usecase

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object BirthdayReminderDateCalculator {
    fun nextBirthday(birthday: LocalDate, today: LocalDate): LocalDate {
        val thisYearBirthday = birthdayInYear(birthday, today.year)
        return if (thisYearBirthday.isBefore(today)) {
            birthdayInYear(birthday, today.year + 1)
        } else {
            thisYearBirthday
        }
    }

    fun daysUntilBirthday(birthday: LocalDate, today: LocalDate): Int {
        val next = nextBirthday(birthday, today)
        return ChronoUnit.DAYS.between(today, next).toInt()
    }

    private fun birthdayInYear(birthday: LocalDate, year: Int): LocalDate {
        val maxDay = YearMonth.of(year, birthday.month).lengthOfMonth()
        val day = birthday.dayOfMonth.coerceAtMost(maxDay)
        return LocalDate.of(year, birthday.month, day)
    }
}
