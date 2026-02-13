package com.zhongxul.birthkeeper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import com.zhongxul.birthkeeper.reminder.EXTRA_OPEN_PERSON_ID
import com.zhongxul.birthkeeper.reminder.EXTRA_REMINDER_LOG_ID
import com.zhongxul.birthkeeper.ui.theme.BirthKeeperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pendingOpenPersonId by mutableStateOf<Long?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
        setContent {
            BirthKeeperTheme {
                BirthKeeperApp(
                    pendingOpenPersonId = pendingOpenPersonId,
                    onPendingOpenPersonConsumed = { pendingOpenPersonId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        pendingOpenPersonId = intent.getLongExtra(EXTRA_OPEN_PERSON_ID, -1L).takeIf { it > 0L }
        val reminderLogId = intent.getLongExtra(EXTRA_REMINDER_LOG_ID, -1L)
        if (reminderLogId > 0L) {
            val app = application as BirthKeeperApplication
            lifecycleScope.launch {
                app.reminderLogRepository.updateStatus(reminderLogId, ReminderLogStatus.CLICKED)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
