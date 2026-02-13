package com.zhongxul.birthkeeper.feature.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderConfig
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository
import com.zhongxul.birthkeeper.core.domain.repository.ReminderLogRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val personRepository: PersonRepository,
    private val reminderLogRepository: ReminderLogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    private var allPeople: List<Person> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                personRepository.observePeople(),
                reminderLogRepository.observeLogs()
            ) { peopleDomain, logs ->
                val personMap = peopleDomain.associateBy { it.id }
                val pending = mutableListOf<ReminderCenterItemUiState>()
                val done = mutableListOf<ReminderCenterItemUiState>()
                logs.forEach { log ->
                    val person = personMap[log.personId] ?: return@forEach
                    val item = ReminderCenterItemUiState(
                        logId = log.id,
                        personId = log.personId,
                        name = person.name,
                        relation = person.relation,
                        targetDate = log.targetDate.toString(),
                        offsetDay = log.offsetDay,
                        status = log.status
                    )
                    if (log.status == ReminderLogStatus.DONE) {
                        done += item
                    } else {
                        pending += item
                    }
                }
                Triple(peopleDomain, pending, done)
            }.collect { (peopleDomain, pending, done) ->
                allPeople = peopleDomain
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        people = peopleDomain.map { it.toReminderPersonItemUiState() },
                        pendingLogs = pending,
                        doneLogs = done
                    )
                }
            }
        }
    }

    fun onEvent(event: ReminderEvent) {
        when (event) {
            is ReminderEvent.EditClicked -> openEditor(event.personId)
            is ReminderEvent.MarkDoneClicked -> updateLogStatus(event.logId, ReminderLogStatus.DONE)
            is ReminderEvent.ReopenClicked -> updateLogStatus(event.logId, ReminderLogStatus.CLICKED)
            ReminderEvent.EditorDismissed -> _uiState.update { it.copy(editor = null) }
            is ReminderEvent.EnabledChanged -> updateEditor { copy(enabled = event.enabled) }
            is ReminderEvent.OffsetsChanged -> updateEditor { copy(offsetsText = event.value) }
            is ReminderEvent.RemindTimeChanged -> updateEditor { copy(remindTimeText = event.value) }
            ReminderEvent.SaveClicked -> saveReminder()
            ReminderEvent.SnackbarConsumed -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun openEditor(personId: Long) {
        val person = allPeople.firstOrNull { it.id == personId } ?: return
        val config = person.reminderConfig
        _uiState.update {
            it.copy(
                editor = ReminderEditorUiState(
                    personId = person.id,
                    name = person.name,
                    enabled = config.enabled,
                    offsetsText = config.offsets.joinToString(","),
                    remindTimeText = config.remindTime.toString()
                )
            )
        }
    }

    private fun saveReminder() {
        val editor = _uiState.value.editor ?: return
        val person = allPeople.firstOrNull { it.id == editor.personId } ?: return
        val offsets = editor.offsetsText.split(",")
            .mapNotNull { value -> value.trim().toIntOrNull() }
            .filter { value -> value >= 0 }
            .distinct()
            .sortedDescending()
        if (offsets.isEmpty()) {
            showSnackbar("\u63d0\u9192\u5929\u6570\u683c\u5f0f\u9519\u8bef\uff0c\u8bf7\u4f7f\u7528\u9017\u53f7\u5206\u9694\u6570\u5b57\uff0c\u4f8b\u5982 7,3,1,0")
            return
        }
        val remindTime = runCatching { LocalTime.parse(editor.remindTimeText.trim()) }.getOrNull()
        if (remindTime == null) {
            showSnackbar("\u63d0\u9192\u65f6\u95f4\u683c\u5f0f\u9519\u8bef\uff0c\u5e94\u4e3a HH:mm\uff0c\u4f8b\u5982 09:00")
            return
        }

        updateEditor { copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                personRepository.upsert(
                    person.copy(
                        reminderConfig = ReminderConfig(
                            offsets = offsets,
                            remindTime = remindTime,
                            enabled = editor.enabled
                        )
                    )
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        editor = null,
                        snackbarMessage = "\u63d0\u9192\u914d\u7f6e\u5df2\u4fdd\u5b58"
                    )
                }
            }.onFailure {
                updateEditor { copy(isSaving = false) }
                showSnackbar("\u63d0\u9192\u914d\u7f6e\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5")
            }
        }
    }

    private fun updateLogStatus(logId: Long, status: ReminderLogStatus) {
        viewModelScope.launch {
            runCatching {
                reminderLogRepository.updateStatus(logId, status)
            }.onFailure {
                showSnackbar("\u66f4\u65b0\u63d0\u9192\u72b6\u6001\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5")
            }
        }
    }

    private fun updateEditor(update: ReminderEditorUiState.() -> ReminderEditorUiState) {
        _uiState.update { state ->
            val editor = state.editor ?: return@update state
            state.copy(editor = editor.update())
        }
    }

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    companion object {
        fun factory(
            personRepository: PersonRepository,
            reminderLogRepository: ReminderLogRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return ReminderViewModel(personRepository, reminderLogRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
