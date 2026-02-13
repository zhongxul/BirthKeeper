package com.zhongxul.birthkeeper.feature.person

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository

const val PERSON_ROUTE = "person"
const val TAG_PERSON_ADD_FAB = "person_add_fab"
const val TAG_PERSON_NAME_INPUT = "person_name_input"
const val TAG_PERSON_RELATION_INPUT = "person_relation_input"
const val TAG_PERSON_BIRTHDAY_INPUT = "person_birthday_input"
const val TAG_PERSON_ID_INPUT = "person_id_input"
const val TAG_PERSON_SAVE_BUTTON = "person_save_button"

@Composable
fun PersonListRoute(
    personRepository: PersonRepository,
    onOpenCapture: () -> Unit,
    capturePrefillName: String?,
    capturePrefillIdNumber: String?,
    onCapturePrefillConsumed: () -> Unit,
    pendingOpenPersonId: Long?,
    onPendingOpenPersonConsumed: () -> Unit
) {
    val viewModel: PersonListViewModel = viewModel(
        factory = PersonListViewModel.factory(personRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onEvent(PersonListEvent.SnackbarConsumed)
    }
    LaunchedEffect(capturePrefillName, capturePrefillIdNumber) {
        if (capturePrefillName.isNullOrBlank() && capturePrefillIdNumber.isNullOrBlank()) {
            return@LaunchedEffect
        }
        viewModel.onEvent(
            PersonListEvent.CaptureResultArrived(
                name = capturePrefillName,
                idNumber = capturePrefillIdNumber
            )
        )
        onCapturePrefillConsumed()
    }
    LaunchedEffect(pendingOpenPersonId) {
        val personId = pendingOpenPersonId ?: return@LaunchedEffect
        viewModel.onEvent(PersonListEvent.EditClicked(personId))
        onPendingOpenPersonConsumed()
    }

    PersonListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onOpenCapture = onOpenCapture
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonListScreen(
    uiState: PersonListUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (PersonListEvent) -> Unit,
    onOpenCapture: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "\u8054\u7cfb\u4eba") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(PersonListEvent.AddClicked) },
                modifier = Modifier.testTag(TAG_PERSON_ADD_FAB)
            ) {
                Text(text = "+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(PersonListEvent.SearchChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "\u641c\u7d22\u59d3\u540d/\u5173\u7cfb/\u5907\u6ce8") },
                singleLine = true
            )
            RelationFilterRow(
                relations = uiState.relations,
                selectedRelation = uiState.selectedRelation,
                onFilterSelected = { onEvent(PersonListEvent.RelationFilterChanged(it)) }
            )
            if (uiState.isLoading) {
                Text(
                    text = "\u52a0\u8f7d\u4e2d...",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (uiState.people.isEmpty()) {
                Text(
                    text = "\u6682\u65e0\u8054\u7cfb\u4eba\uff0c\u70b9\u53f3\u4e0b\u89d2 + \u65b0\u589e",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.people, key = { it.id }) { person ->
                        PersonCard(
                            person = person,
                            onEdit = { onEvent(PersonListEvent.EditClicked(person.id)) },
                            onDelete = { onEvent(PersonListEvent.DeleteClicked(person.id)) }
                        )
                    }
                }
            }
        }
    }

    val editor = uiState.editor
    if (editor != null) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(PersonListEvent.EditorDismissed) }
        ) {
            EditorSheet(
                editor = editor,
                onEvent = onEvent,
                onOpenCapture = onOpenCapture
            )
        }
    }

    if (uiState.pendingDeletePersonId != null) {
        AlertDialog(
            onDismissRequest = { onEvent(PersonListEvent.DeleteCanceled) },
            title = { Text(text = "\u5220\u9664\u63d0\u793a") },
            text = { Text(text = "\u786e\u5b9a\u5220\u9664\u8be5\u8054\u7cfb\u4eba\u5417\uff1f") },
            confirmButton = {
                TextButton(onClick = { onEvent(PersonListEvent.DeleteConfirmed) }) {
                    Text(text = "\u5220\u9664")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PersonListEvent.DeleteCanceled) }) {
                    Text(text = "\u53d6\u6d88")
                }
            }
        )
    }
}

@Composable
private fun RelationFilterRow(
    relations: List<String>,
    selectedRelation: String?,
    onFilterSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedRelation == null,
            onClick = { onFilterSelected(null) },
            label = { Text(text = "\u5168\u90e8") }
        )
        relations.forEach { relation ->
            FilterChip(
                selected = selectedRelation == relation,
                onClick = { onFilterSelected(relation) },
                label = { Text(text = relation) }
            )
        }
    }
}

@Composable
private fun PersonCard(
    person: PersonListItemUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${person.name} · ${person.relation}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "\u751f\u65e5\uff1a${person.birthday}  \u6027\u522b\uff1a${person.gender.toUiText()}  \u5e74\u9f84\uff1a${person.age}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!person.idNumberMasked.isNullOrBlank()) {
                Text(
                    text = "\u8eab\u4efd\u8bc1\uff1a${person.idNumberMasked}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!person.note.isNullOrBlank()) {
                Text(
                    text = "\u5907\u6ce8\uff1a${person.note}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text(text = "\u7f16\u8f91")
                }
                TextButton(onClick = onDelete) {
                    Text(text = "\u5220\u9664")
                }
            }
        }
    }
}

@Composable
private fun EditorSheet(
    editor: PersonEditorUiState,
    onEvent: (PersonListEvent) -> Unit,
    onOpenCapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (editor.personId == null) "\u65b0\u589e\u8054\u7cfb\u4eba" else "\u7f16\u8f91\u8054\u7cfb\u4eba",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = editor.name,
            onValueChange = { onEvent(PersonListEvent.NameChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_PERSON_NAME_INPUT),
            label = { Text(text = "\u59d3\u540d*") },
            singleLine = true
        )
        OutlinedTextField(
            value = editor.relation,
            onValueChange = { onEvent(PersonListEvent.RelationChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_PERSON_RELATION_INPUT),
            label = { Text(text = "\u5173\u7cfb*") },
            singleLine = true
        )
        OutlinedTextField(
            value = editor.birthday,
            onValueChange = { onEvent(PersonListEvent.BirthdayChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_PERSON_BIRTHDAY_INPUT),
            label = { Text(text = "\u751f\u65e5*(YYYY-MM-DD)") },
            singleLine = true
        )
        OutlinedTextField(
            value = editor.idNumber,
            onValueChange = { onEvent(PersonListEvent.IdNumberChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_PERSON_ID_INPUT),
            label = { Text(text = "\u8eab\u4efd\u8bc1\u53f7") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            visualTransformation = if (editor.isIdNumberVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = { onEvent(PersonListEvent.ToggleIdNumberVisibility) }
                ) {
                    Text(text = if (editor.isIdNumberVisible) "\u9690\u85cf" else "\u663e\u793a")
                }
            },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "\u6027\u522b\uff1a${editor.gender.toUiText()}  \u5e74\u9f84\uff1a${editor.age?.toString() ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenCapture,
                    modifier = Modifier.size(width = 96.dp, height = 40.dp)
                ) {
                    Text(text = "OCR")
                }
                Button(
                    onClick = { onEvent(PersonListEvent.ParseIdCardClicked) },
                    modifier = Modifier.size(width = 112.dp, height = 40.dp)
                ) {
                    Text(text = "\u89e3\u6790\u8bc1\u4ef6")
                }
            }
        }
        if (!editor.parseHint.isNullOrBlank()) {
            Text(
                text = editor.parseHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        OutlinedTextField(
            value = editor.note,
            onValueChange = { onEvent(PersonListEvent.NoteChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "\u5907\u6ce8") },
            minLines = 3
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { onEvent(PersonListEvent.EditorDismissed) }) {
                Text(text = "\u53d6\u6d88")
            }
            Button(
                onClick = { onEvent(PersonListEvent.SaveClicked) },
                modifier = Modifier.testTag(TAG_PERSON_SAVE_BUTTON),
                enabled = !editor.isSaving
            ) {
                Text(text = if (editor.isSaving) "\u4fdd\u5b58\u4e2d..." else "\u4fdd\u5b58")
            }
        }
    }
}
