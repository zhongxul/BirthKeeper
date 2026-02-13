package com.zhongxul.birthkeeper.feature.reminder

sealed interface ReminderEvent {
    data class EditClicked(val personId: Long) : ReminderEvent
    data class MarkDoneClicked(val logId: Long) : ReminderEvent
    data class ReopenClicked(val logId: Long) : ReminderEvent
    data object EditorDismissed : ReminderEvent
    data class EnabledChanged(val enabled: Boolean) : ReminderEvent
    data class OffsetsChanged(val value: String) : ReminderEvent
    data class RemindTimeChanged(val value: String) : ReminderEvent
    data object SaveClicked : ReminderEvent
    data object SnackbarConsumed : ReminderEvent
}
