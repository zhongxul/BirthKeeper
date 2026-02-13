package com.zhongxul.birthkeeper

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zhongxul.birthkeeper.feature.capture.CAPTURE_ROUTE
import com.zhongxul.birthkeeper.feature.capture.CapturePrefillResult
import com.zhongxul.birthkeeper.feature.capture.CaptureRoute
import com.zhongxul.birthkeeper.feature.person.PERSON_ROUTE
import com.zhongxul.birthkeeper.feature.person.PersonListRoute
import com.zhongxul.birthkeeper.feature.reminder.REMINDER_ROUTE
import com.zhongxul.birthkeeper.feature.reminder.ReminderRoute

private data class BottomDestination(
    val route: String,
    val label: String
)

const val TAG_BOTTOM_NAV_PERSON = "bottom_nav_person"
const val TAG_BOTTOM_NAV_CAPTURE = "bottom_nav_capture"
const val TAG_BOTTOM_NAV_REMINDER = "bottom_nav_reminder"

@Composable
fun BirthKeeperApp(
    pendingOpenPersonId: Long? = null,
    onPendingOpenPersonConsumed: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as BirthKeeperApplication
    val navController = rememberNavController()
    var capturePrefill by remember { mutableStateOf<CapturePrefillResult?>(null) }
    val destinations = listOf(
        BottomDestination(PERSON_ROUTE, "\u8054\u7cfb\u4eba"),
        BottomDestination(CAPTURE_ROUTE, "\u626b\u63cf"),
        BottomDestination(REMINDER_ROUTE, "\u63d0\u9192")
    )
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(
                            when (destination.route) {
                                PERSON_ROUTE -> TAG_BOTTOM_NAV_PERSON
                                CAPTURE_ROUTE -> TAG_BOTTOM_NAV_CAPTURE
                                REMINDER_ROUTE -> TAG_BOTTOM_NAV_REMINDER
                                else -> destination.route
                            }
                        ),
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(text = destination.label) },
                        icon = { Text(text = destination.label.take(1)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PERSON_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(PERSON_ROUTE) {
                PersonListRoute(
                    personRepository = app.personRepository,
                    onOpenCapture = {
                        navController.navigate(CAPTURE_ROUTE)
                    },
                    capturePrefillName = capturePrefill?.name,
                    capturePrefillIdNumber = capturePrefill?.idNumber,
                    onCapturePrefillConsumed = {
                        capturePrefill = null
                    },
                    pendingOpenPersonId = pendingOpenPersonId,
                    onPendingOpenPersonConsumed = onPendingOpenPersonConsumed
                )
            }
            composable(CAPTURE_ROUTE) {
                CaptureRoute(
                    onBack = { navController.popBackStack() },
                    onApplyResult = { result ->
                        capturePrefill = result
                        navController.popBackStack()
                    }
                )
            }
            composable(REMINDER_ROUTE) {
                ReminderRoute(
                    personRepository = app.personRepository,
                    reminderLogRepository = app.reminderLogRepository,
                    backupRepository = app.backupRepository
                )
            }
        }
    }
}
