package com.zhongxul.birthkeeper.core.domain.validator

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object IdCardValidator {
    private val idRegex = Regex("^\\d{17}[\\dXx]$")
    private val weight = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    private val mapping = charArrayOf('1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2')
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun isFormatValid(id: String): Boolean = idRegex.matches(id)

    fun isChecksumValid(id: String): Boolean {
        if (!isFormatValid(id)) return false
        val sum = id.take(17).mapIndexed { index, c ->
            c.digitToInt() * weight[index]
        }.sum()
        val expected = mapping[sum % 11]
        return expected == id.last().uppercaseChar()
    }

    fun parseBirthDate(id: String): LocalDate? {
        if (!isFormatValid(id)) return null
        return try {
            LocalDate.parse(id.substring(6, 14), dateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

