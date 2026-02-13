package com.zhongxul.birthkeeper.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdCardOcrParserTest {

    @Test
    fun `应从OCR文本中提取姓名和身份证号`() {
        val rawText = """
            中华人民共和国居民身份证
            姓名 张三
            公民身份号码 11010519491231002X
        """.trimIndent()

        val result = IdCardOcrParser.parse(rawText)

        assertEquals("张三", result.name)
        assertEquals("11010519491231002X", result.idNumber)
    }

    @Test
    fun `当只有无效身份证号时返回空身份证`() {
        val rawText = """
            姓名 李四
            公民身份号码 110105194912310021
        """.trimIndent()

        val result = IdCardOcrParser.parse(rawText) { false }

        assertEquals("李四", result.name)
        assertNull(result.idNumber)
    }
}
