package com.zhongxul.birthkeeper.feature.reminder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhongxul.birthkeeper.core.domain.repository.BackupImportMode
import com.zhongxul.birthkeeper.core.domain.repository.BackupRepository
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository
import com.zhongxul.birthkeeper.core.domain.repository.ReminderLogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

const val REMINDER_ROUTE = "reminder"
const val TAG_REMINDER_MARK_DONE_PREFIX = "reminder_mark_done_"
const val TAG_REMINDER_REOPEN_PREFIX = "reminder_reopen_"

fun reminderMarkDoneTag(logId: Long): String = "$TAG_REMINDER_MARK_DONE_PREFIX$logId"
fun reminderReopenTag(logId: Long): String = "$TAG_REMINDER_REOPEN_PREFIX$logId"

@Composable
fun ReminderRoute(
    personRepository: PersonRepository,
    reminderLogRepository: ReminderLogRepository,
    backupRepository: BackupRepository
) {
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModel.factory(personRepository, reminderLogRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImportMode by remember { mutableStateOf<BackupImportMode?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val payload = backupRepository.exportEncryptedBackup()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(payload)
                } ?: error("无法写入目标文件")
            }.onSuccess {
                snackbarHostState.showSnackbar("备份导出成功")
            }.onFailure {
                snackbarHostState.showSnackbar("备份导出失败：${it.message.orEmpty()}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val mode = pendingImportMode
        pendingImportMode = null
        if (uri == null || mode == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val payload = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { reader -> reader.readText() }
                    ?: error("无法读取备份文件")
                backupRepository.importEncryptedBackup(payload, mode)
            }.onSuccess { result ->
                snackbarHostState.showSnackbar(
                    "导入完成：新增 ${result.importedCount}，更新 ${result.updatedCount}，跳过 ${result.skippedCount}"
                )
            }.onFailure {
                snackbarHostState.showSnackbar("导入失败：${it.message.orEmpty()}")
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onEvent(ReminderEvent.SnackbarConsumed)
    }

    ReminderScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onExport = {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
            exportLauncher.launch("birthkeeper_backup_$timestamp.bkup")
        },
        onImportOverwrite = {
            pendingImportMode = BackupImportMode.OVERWRITE
            importLauncher.launch(arrayOf("*/*"))
        },
        onImportMerge = {
            pendingImportMode = BackupImportMode.MERGE
            importLauncher.launch(arrayOf("*/*"))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderScreen(
    uiState: ReminderUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ReminderEvent) -> Unit,
    onExport: () -> Unit,
    onImportOverwrite: () -> Unit,
    onImportMerge: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "提醒中心") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Text(
                text = "加载中...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionTitle("备份恢复")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(modifier = Modifier.weight(1f), onClick = onExport) {
                            Text(text = "导出备份")
                        }
                        Button(modifier = Modifier.weight(1f), onClick = onImportMerge) {
                            Text(text = "导入合并")
                        }
                    }
                    TextButton(onClick = onImportOverwrite) {
                        Text(text = "导入覆盖（危险操作）")
                    }
                }

                item {
                    SectionTitle("提醒配置")
                }
                if (uiState.people.isEmpty()) {
                    item {
                        Text(text = "暂无联系人，请先在联系人页新增")
                    }
                } else {
                    items(uiState.people, key = { it.personId }) { person ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "${person.name} · ${person.relation}")
                                Text(text = "生日：${person.birthday}")
                                Text(text = person.configSummary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            onEvent(ReminderEvent.EditClicked(person.personId))
                                        }
                                    ) {
                                        Text(text = "编辑")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("待处理提醒")
                }
                if (uiState.pendingLogs.isEmpty()) {
                    item {
                        Text(text = "暂无待处理提醒")
                    }
                } else {
                    items(uiState.pendingLogs, key = { it.logId }) { log ->
                        ReminderLogCard(
                            item = log,
                            actionText = "标记已处理",
                            actionTag = reminderMarkDoneTag(log.logId),
                            onAction = { onEvent(ReminderEvent.MarkDoneClicked(log.logId)) }
                        )
                    }
                }

                item {
                    SectionTitle("已处理提醒")
                }
                if (uiState.doneLogs.isEmpty()) {
                    item {
                        Text(text = "暂无已处理提醒")
                    }
                } else {
                    items(uiState.doneLogs, key = { it.logId }) { log ->
                        ReminderLogCard(
                            item = log,
                            actionText = "重新打开",
                            actionTag = reminderReopenTag(log.logId),
                            onAction = { onEvent(ReminderEvent.ReopenClicked(log.logId)) }
                        )
                    }
                }
            }
        }
    }

    val editor = uiState.editor
    if (editor != null) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(ReminderEvent.EditorDismissed) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "编辑提醒：${editor.name}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "启用提醒")
                    Switch(
                        checked = editor.enabled,
                        onCheckedChange = { checked ->
                            onEvent(ReminderEvent.EnabledChanged(checked))
                        }
                    )
                }
                OutlinedTextField(
                    value = editor.offsetsText,
                    onValueChange = { onEvent(ReminderEvent.OffsetsChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "提前天数（逗号分隔）") },
                    placeholder = { Text(text = "例如：7,3,1,0") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = editor.remindTimeText,
                    onValueChange = { onEvent(ReminderEvent.RemindTimeChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "提醒时间（HH:mm）") },
                    placeholder = { Text(text = "例如：09:00") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onEvent(ReminderEvent.EditorDismissed) }) {
                        Text(text = "取消")
                    }
                    Button(
                        onClick = { onEvent(ReminderEvent.SaveClicked) },
                        enabled = !editor.isSaving
                    ) {
                        Text(text = if (editor.isSaving) "保存中..." else "保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun ReminderLogCard(
    item: ReminderCenterItemUiState,
    actionText: String,
    actionTag: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "${item.name} · ${item.relation}")
            Text(text = "目标日期：${item.targetDate}（提前 ${item.offsetDay} 天）")
            Text(text = "状态：${item.statusText}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.testTag(actionTag),
                    onClick = onAction
                ) {
                    Text(text = actionText)
                }
            }
        }
    }
}
