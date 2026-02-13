package com.zhongxul.birthkeeper.core.domain.usecase

import com.zhongxul.birthkeeper.core.domain.model.Gender
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseIdCardUseCaseTest {
    private val useCase = ParseIdCardUseCase()

    @Test
    fun `给定合法身份证 应返回生日性别年龄`() {
        val result = useCase(
            idCard = "11010519491231002X",
            today = LocalDate.of(2026, 2, 13)
        )

        assertTrue(result is ParseIdCardResult.Valid)
        val valid = result as ParseIdCardResult.Valid
        assertEquals(LocalDate.of(1949, 12, 31), valid.birthDate)
        assertEquals(Gender.FEMALE, valid.gender)
        assertEquals(76, valid.age)
    }

    @Test
    fun `给定校验位错误身份证 应返回无效`() {
        val result = useCase(
            idCard = "110105194912310020",
            today = LocalDate.of(2026, 2, 13)
        )

        assertTrue(result is ParseIdCardResult.Invalid)
    }

    @Test
    fun `给定格式错误身份证 应返回无效`() {
        val result = useCase(
            idCard = "123456",
            today = LocalDate.of(2026, 2, 13)
        )

        assertTrue(result is ParseIdCardResult.Invalid)
    }
}

