package com.zhongxul.birthkeeper.core.domain.usecase

import com.zhongxul.birthkeeper.core.domain.model.Gender
import com.zhongxul.birthkeeper.core.domain.validator.IdCardValidator
import java.time.LocalDate
import java.time.Period

class ParseIdCardUseCase {
    operator fun invoke(idCard: String, today: LocalDate = LocalDate.now()): ParseIdCardResult {
        if (!IdCardValidator.isFormatValid(idCard)) {
            return ParseIdCardResult.Invalid("身份证格式错误")
        }
        if (!IdCardValidator.isChecksumValid(idCard)) {
            return ParseIdCardResult.Invalid("身份证校验位错误")
        }
        val birthDate = IdCardValidator.parseBirthDate(idCard)
            ?: return ParseIdCardResult.Invalid("身份证出生日期无效")
        if (birthDate.isAfter(today)) {
            return ParseIdCardResult.Invalid("身份证出生日期晚于当前日期")
        }

        val gender = if (idCard[16].digitToInt() % 2 == 1) {
            Gender.MALE
        } else {
            Gender.FEMALE
        }
        val age = Period.between(birthDate, today).years
        return ParseIdCardResult.Valid(
            birthDate = birthDate,
            gender = gender,
            age = age
        )
    }
}

sealed interface ParseIdCardResult {
    data class Valid(
        val birthDate: LocalDate,
        val gender: Gender,
        val age: Int
    ) : ParseIdCardResult

    data class Invalid(val reason: String) : ParseIdCardResult
}

