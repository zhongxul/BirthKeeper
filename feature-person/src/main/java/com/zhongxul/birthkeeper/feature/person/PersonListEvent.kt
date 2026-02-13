package com.zhongxul.birthkeeper.feature.person

sealed interface PersonListEvent {
    data class SearchChanged(val query: String) : PersonListEvent
    data class RelationFilterChanged(val relation: String?) : PersonListEvent

    data object AddClicked : PersonListEvent
    data class EditClicked(val personId: Long) : PersonListEvent
    data class CaptureResultArrived(val name: String?, val idNumber: String?) : PersonListEvent

    data class DeleteClicked(val personId: Long) : PersonListEvent
    data object DeleteConfirmed : PersonListEvent
    data object DeleteCanceled : PersonListEvent

    data object EditorDismissed : PersonListEvent
    data class NameChanged(val value: String) : PersonListEvent
    data class RelationChanged(val value: String) : PersonListEvent
    data class BirthdayChanged(val value: String) : PersonListEvent
    data class IdNumberChanged(val value: String) : PersonListEvent
    data object ToggleIdNumberVisibility : PersonListEvent
    data class NoteChanged(val value: String) : PersonListEvent
    data object ParseIdCardClicked : PersonListEvent
    data object SaveClicked : PersonListEvent

    data object SnackbarConsumed : PersonListEvent
}
