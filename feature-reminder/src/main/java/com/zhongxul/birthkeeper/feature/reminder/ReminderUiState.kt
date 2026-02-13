package com.zhongxul.birthkeeper.feature.reminder

import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus

data class ReminderUiState(
    val isLoading: Boolean = true,
    val people: List<ReminderPersonItemUiState> = emptyList(),
    val pendingLogs: List<ReminderCenterItemUiState> = emptyList(),
    val doneLogs: List<ReminderCenterItemUiState> = emptyList(),
    val editor: ReminderEditorUiState? = null,
    val snackbarMessage: String? = null
)

data class ReminderPersonItemUiState(
    val personId: Long,
    val name: String,
    val relation: String,
    val birthday: String,
    val configSummary: String
)

data class ReminderCenterItemUiState(
    val logId: Long,
    val personId: Long,
    val name: String,
    val relation: String,
    val targetDate: String,
    val offsetDay: Int,
    val status: ReminderLogStatus
) {
    val statusText: String
        get() = when (status) {
            ReminderLogStatus.PLANNED -> "待触发"
            ReminderLogStatus.SENT -> "已通知"
            ReminderLogStatus.CLICKED -> "已点击"
            ReminderLogStatus.DONE -> "已处理"
        }
}

data class ReminderEditorUiState(
    val personId: Long,
    val name: String,
    val enabled: Boolean,
    val offsetsText: String,
    val remindTimeText: String,
    val isSaving: Boolean = false
)

fun Person.toReminderPersonItemUiState(): ReminderPersonItemUiState {
    val config = reminderConfig
    val summary = if (config.enabled) {
        "\u63d0\u524d ${config.offsets.joinToString("/")} \u5929\uff0c\u65f6\u95f4 ${config.remindTime}"
    } else {
        "\u5df2\u5173\u95ed\u63d0\u9192"
    }
    return ReminderPersonItemUiState(
        personId = id,
        name = name,
        relation = relation,
        birthday = birthdaySolar.toString(),
        configSummary = summary
    )
}
