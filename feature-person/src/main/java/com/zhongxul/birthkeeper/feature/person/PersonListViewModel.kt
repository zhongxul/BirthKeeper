package com.zhongxul.birthkeeper.feature.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zhongxul.birthkeeper.core.common.util.MaskingUtils
import com.zhongxul.birthkeeper.core.domain.model.Gender
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderConfig
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository
import com.zhongxul.birthkeeper.core.domain.usecase.ParseIdCardResult
import com.zhongxul.birthkeeper.core.domain.usecase.ParseIdCardUseCase
import java.time.LocalDate
import java.time.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonListViewModel(
    private val personRepository: PersonRepository,
    private val parseIdCardUseCase: ParseIdCardUseCase = ParseIdCardUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonListUiState())
    val uiState: StateFlow<PersonListUiState> = _uiState.asStateFlow()

    private var allPeople: List<Person> = emptyList()

    init {
        viewModelScope.launch {
            personRepository.observePeople().collect { people ->
                allPeople = people
                refreshList()
            }
        }
    }

    fun onEvent(event: PersonListEvent) {
        when (event) {
            is PersonListEvent.SearchChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                refreshList()
            }

            is PersonListEvent.RelationFilterChanged -> {
                _uiState.update { it.copy(selectedRelation = event.relation) }
                refreshList()
            }

            PersonListEvent.AddClicked -> openEditor(person = null)
            is PersonListEvent.EditClicked -> openEditor(person = allPeople.firstOrNull { it.id == event.personId })
            is PersonListEvent.CaptureResultArrived -> applyCaptureResult(event.name, event.idNumber)

            is PersonListEvent.DeleteClicked -> {
                _uiState.update { it.copy(pendingDeletePersonId = event.personId) }
            }

            PersonListEvent.DeleteCanceled -> {
                _uiState.update { it.copy(pendingDeletePersonId = null) }
            }

            PersonListEvent.DeleteConfirmed -> {
                val personId = _uiState.value.pendingDeletePersonId ?: return
                _uiState.update { it.copy(pendingDeletePersonId = null) }
                viewModelScope.launch {
                    runCatching {
                        personRepository.softDelete(personId)
                    }.onFailure {
                        showSnackbar("\u5220\u9664\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5")
                    }
                }
            }

            PersonListEvent.EditorDismissed -> {
                _uiState.update { it.copy(editor = null) }
            }

            is PersonListEvent.NameChanged -> updateEditor { copy(name = event.value) }
            is PersonListEvent.RelationChanged -> updateEditor { copy(relation = event.value) }
            is PersonListEvent.BirthdayChanged -> updateEditor { copy(birthday = event.value) }
            is PersonListEvent.IdNumberChanged -> updateEditor { copy(idNumber = event.value, parseHint = null) }
            PersonListEvent.ToggleIdNumberVisibility -> {
                updateEditor { copy(isIdNumberVisible = !isIdNumberVisible) }
            }
            is PersonListEvent.NoteChanged -> updateEditor { copy(note = event.value) }

            PersonListEvent.ParseIdCardClicked -> parseIdCard()
            PersonListEvent.SaveClicked -> saveEditor()
            PersonListEvent.SnackbarConsumed -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun refreshList() {
        val state = _uiState.value
        val searchKeyword = state.searchQuery.trim()
        val relations = allPeople
            .map { it.relation.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val selectedRelation = state.selectedRelation?.takeIf { relation -> relations.contains(relation) }
        val filteredPeople = allPeople.filter { person ->
            val matchSearch = if (searchKeyword.isBlank()) {
                true
            } else {
                person.name.contains(searchKeyword, ignoreCase = true) ||
                    person.relation.contains(searchKeyword, ignoreCase = true) ||
                    (person.note?.contains(searchKeyword, ignoreCase = true) == true)
            }
            val matchRelation = selectedRelation == null || person.relation == selectedRelation
            matchSearch && matchRelation
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                selectedRelation = selectedRelation,
                relations = relations,
                people = filteredPeople.map { person -> person.toItemUiState() }
            )
        }
    }

    private fun openEditor(person: Person?) {
        _uiState.update {
            it.copy(
                editor = person?.toEditorUiState() ?: PersonEditorUiState(
                    relation = it.selectedRelation.orEmpty(),
                    birthday = LocalDate.now().toString()
                )
            )
        }
    }

    private fun parseIdCard() {
        val editor = _uiState.value.editor ?: return
        val idNumber = editor.idNumber.trim().uppercase()
        if (idNumber.isBlank()) {
            showSnackbar("\u8bf7\u5148\u8f93\u5165\u8eab\u4efd\u8bc1\u53f7")
            return
        }
        when (val result = parseIdCardUseCase(idNumber)) {
            is ParseIdCardResult.Valid -> {
                updateEditor {
                    copy(
                        idNumber = idNumber,
                        birthday = result.birthDate.toString(),
                        gender = result.gender,
                        age = result.age,
                        parseHint = "\u5df2\u6839\u636e\u8eab\u4efd\u8bc1\u56de\u586b\u751f\u65e5/\u6027\u522b/\u5e74\u9f84"
                    )
                }
            }

            is ParseIdCardResult.Invalid -> {
                updateEditor {
                    copy(
                        idNumber = idNumber,
                        parseHint = "\u8eab\u4efd\u8bc1\u89e3\u6790\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u53f7\u7801"
                    )
                }
            }
        }
    }

    private fun applyCaptureResult(name: String?, idNumber: String?) {
        if (name.isNullOrBlank() && idNumber.isNullOrBlank()) {
            return
        }
        if (_uiState.value.editor == null) {
            openEditor(person = null)
        }
        updateEditor {
            copy(
                name = if (name.isNullOrBlank()) this.name else name,
                idNumber = if (idNumber.isNullOrBlank()) this.idNumber else idNumber.uppercase(),
                isIdNumberVisible = true,
                parseHint = null
            )
        }
        if (!idNumber.isNullOrBlank()) {
            parseIdCard()
        }
    }

    private fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        val name = editor.name.trim()
        if (name.isBlank()) {
            showSnackbar("\u59d3\u540d\u4e0d\u80fd\u4e3a\u7a7a")
            return
        }
        val relation = editor.relation.trim()
        if (relation.isBlank()) {
            showSnackbar("\u5173\u7cfb\u4e0d\u80fd\u4e3a\u7a7a")
            return
        }
        val birthday = runCatching { LocalDate.parse(editor.birthday.trim()) }.getOrNull()
        if (birthday == null) {
            showSnackbar("\u751f\u65e5\u683c\u5f0f\u5e94\u4e3a YYYY-MM-DD")
            return
        }

        val existing = editor.personId?.let { personId ->
            allPeople.firstOrNull { person -> person.id == personId }
        }
        val now = System.currentTimeMillis()
        val person = Person(
            id = existing?.id ?: 0L,
            name = name,
            idNumber = editor.idNumber.trim().ifBlank { null },
            birthdaySolar = birthday,
            birthdayLunar = existing?.birthdayLunar,
            gender = editor.gender,
            relation = relation,
            note = editor.note.trim().ifBlank { null },
            avatarUri = existing?.avatarUri,
            reminderConfig = existing?.reminderConfig ?: ReminderConfig(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = existing?.updatedAt ?: now
        )

        updateEditor { copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                personRepository.upsert(person)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        editor = null,
                        snackbarMessage = "\u5df2\u4fdd\u5b58"
                    )
                }
            }.onFailure {
                updateEditor { copy(isSaving = false) }
                showSnackbar("\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5")
            }
        }
    }

    private fun updateEditor(update: PersonEditorUiState.() -> PersonEditorUiState) {
        _uiState.update { state ->
            val editor = state.editor ?: return@update state
            state.copy(editor = editor.update())
        }
    }

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    private fun Person.toItemUiState(): PersonListItemUiState {
        return PersonListItemUiState(
            id = id,
            name = name,
            relation = relation,
            birthday = birthdaySolar.toString(),
            gender = gender,
            age = calcAge(birthdaySolar),
            idNumberMasked = idNumber?.let { value -> MaskingUtils.maskIdNumber(value) },
            note = note
        )
    }

    private fun Person.toEditorUiState(): PersonEditorUiState {
        return PersonEditorUiState(
            personId = id,
            name = name,
            relation = relation,
            birthday = birthdaySolar.toString(),
            idNumber = idNumber.orEmpty(),
            isIdNumberVisible = idNumber.isNullOrBlank(),
            note = note.orEmpty(),
            gender = gender,
            age = calcAge(birthdaySolar)
        )
    }

    private fun calcAge(birthday: LocalDate): Int {
        val years = Period.between(birthday, LocalDate.now()).years
        return years.coerceAtLeast(0)
    }

    companion object {
        fun factory(personRepository: PersonRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PersonListViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return PersonListViewModel(personRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

fun Gender.toUiText(): String {
    return when (this) {
        Gender.MALE -> "\u7537"
        Gender.FEMALE -> "\u5973"
        Gender.UNKNOWN -> "\u672a\u77e5"
    }
}
