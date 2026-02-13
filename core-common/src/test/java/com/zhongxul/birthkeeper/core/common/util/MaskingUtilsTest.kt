package com.zhongxul.birthkeeper.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskingUtilsTest {

    @Test
    fun maskIdNumber_whenLengthEnough_shouldKeepPrefixAndSuffix() {
        val masked = MaskingUtils.maskIdNumber("110101199001011234")
        assertEquals("110***********1234", masked)
    }

    @Test
    fun maskIdNumber_whenTooShort_shouldReturnOriginal() {
        val original = "1234567"
        assertEquals(original, MaskingUtils.maskIdNumber(original))
    }
}
