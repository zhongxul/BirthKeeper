package com.zhongxul.birthkeeper.feature.person

import com.zhongxul.birthkeeper.core.domain.model.Gender

data class PersonListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedRelation: String? = null,
    val relations: List<String> = emptyList(),
    val people: List<PersonListItemUiState> = emptyList(),
    val editor: PersonEditorUiState? = null,
    val pendingDeletePersonId: Long? = null,
    val snackbarMessage: String? = null
)

data class PersonListItemUiState(
    val id: Long,
    val name: String,
    val relation: String,
    val birthday: String,
    val gender: Gender,
    val age: Int,
    val idNumberMasked: String?,
    val note: String?
)

data class PersonEditorUiState(
    val personId: Long? = null,
    val name: String = "",
    val relation: String = "",
    val birthday: String = "",
    val idNumber: String = "",
    val isIdNumberVisible: Boolean = true,
    val note: String = "",
    val gender: Gender = Gender.UNKNOWN,
    val age: Int? = null,
    val parseHint: String? = null,
    val isSaving: Boolean = false
)
