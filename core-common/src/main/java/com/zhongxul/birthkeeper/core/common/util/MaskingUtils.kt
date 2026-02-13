package com.zhongxul.birthkeeper.core.common.util

object MaskingUtils {
    fun maskIdNumber(idNumber: String): String {
        if (idNumber.length < 8) return idNumber
        val prefix = idNumber.take(3)
        val suffix = idNumber.takeLast(4)
        val stars = "*".repeat(idNumber.length - 7)
        return "$prefix$stars$suffix"
    }
}

