package com.zhongxul.birthkeeper.feature.capture

data class IdCardOcrParseResult(
    val name: String?,
    val idNumber: String?,
    val idCandidates: List<String>
)

object IdCardOcrParser {
    private val idRegex = Regex("(?<!\\d)(\\d{17}[\\dXx])(?!\\d)")
    private val nameRegex = Regex("姓名\\s*[:：]?\\s*([\\u4E00-\\u9FA5·]{2,20})")
    private val noisyLineKeywords = listOf(
        "中华人民共和国",
        "居民身份证",
        "公民身份号码",
        "住址",
        "签发机关",
        "有效期限",
        "出生"
    )

    fun parse(
        rawText: String,
        isIdNumberValid: (String) -> Boolean = { true }
    ): IdCardOcrParseResult {
        val normalizedText = rawText.replace(" ", "")
        val idCandidates = idRegex
            .findAll(normalizedText)
            .map { result -> result.groupValues[1].uppercase() }
            .distinct()
            .toList()
        val validId = idCandidates.firstOrNull { candidate -> isIdNumberValid(candidate) }
        val idNumber = validId

        val labeledName = nameRegex.find(rawText)?.groupValues?.getOrNull(1)
        val fallbackName = rawText
            .lineSequence()
            .map { line -> line.trim().replace(" ", "") }
            .filter { line ->
                line.length in 2..6 &&
                    line.all { ch -> ch == '·' || ch in '\u4E00'..'\u9FA5' } &&
                    noisyLineKeywords.none { keyword -> line.contains(keyword) }
            }
            .firstOrNull()
        val name = labeledName ?: fallbackName

        return IdCardOcrParseResult(
            name = name,
            idNumber = idNumber,
            idCandidates = idCandidates
        )
    }
}
