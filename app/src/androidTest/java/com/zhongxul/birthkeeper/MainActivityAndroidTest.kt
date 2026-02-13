package com.zhongxul.birthkeeper

import android.Manifest
import android.content.Intent
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.zhongxul.birthkeeper.core.domain.model.Gender
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderConfig
import com.zhongxul.birthkeeper.core.domain.model.ReminderLog
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import com.zhongxul.birthkeeper.feature.capture.TAG_CAPTURE_APPLY_BUTTON
import com.zhongxul.birthkeeper.feature.capture.TAG_CAPTURE_ID_INPUT
import com.zhongxul.birthkeeper.feature.capture.TAG_CAPTURE_NAME_INPUT
import com.zhongxul.birthkeeper.feature.person.TAG_PERSON_ADD_FAB
import com.zhongxul.birthkeeper.feature.person.TAG_PERSON_ID_INPUT
import com.zhongxul.birthkeeper.feature.person.TAG_PERSON_NAME_INPUT
import com.zhongxul.birthkeeper.feature.person.TAG_PERSON_RELATION_INPUT
import com.zhongxul.birthkeeper.feature.person.TAG_PERSON_SAVE_BUTTON
import com.zhongxul.birthkeeper.feature.reminder.reminderMarkDoneTag
import com.zhongxul.birthkeeper.reminder.BirthdayReminderAlarmReceiver
import com.zhongxul.birthkeeper.reminder.EXTRA_ALARM_BIRTHDAY
import com.zhongxul.birthkeeper.reminder.EXTRA_ALARM_OFFSET_DAY
import com.zhongxul.birthkeeper.reminder.EXTRA_ALARM_PERSON_ID
import com.zhongxul.birthkeeper.reminder.EXTRA_ALARM_PERSON_NAME
import com.zhongxul.birthkeeper.reminder.EXTRA_ALARM_PERSON_RELATION
import com.zhongxul.birthkeeper.reminder.EXTRA_OPEN_PERSON_ID
import com.zhongxul.birthkeeper.reminder.EXTRA_REMINDER_LOG_ID
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityAndroidTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        clearAllPeople()
    }

    @Test
    fun addContactFlow_shouldCreateAndDisplayContact() {
        composeRule.onNodeWithTag(TAG_PERSON_ADD_FAB).performClick()

        composeRule.onNodeWithTag(TAG_PERSON_NAME_INPUT).performTextInput("e2e_contact_01")
        composeRule.onNodeWithTag(TAG_PERSON_RELATION_INPUT).performTextInput("friend")
        composeRule.onNodeWithTag(TAG_PERSON_SAVE_BUTTON).performClick()

        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithTag(TAG_PERSON_NAME_INPUT).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun notificationIntent_shouldOpenEditorForTargetPerson() {
        val targetId = 20260213L
        val targetName = "intent_target_01"
        insertPerson(
            id = targetId,
            name = targetName,
            relation = "family"
        )

        composeRule.activityRule.scenario.onActivity { activity ->
            val intent = Intent(activity, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_PERSON_ID, targetId)
            }
            val handleIntentMethod = MainActivity::class.java.getDeclaredMethod("handleIntent", Intent::class.java)
            handleIntentMethod.isAccessible = true
            handleIntentMethod.invoke(activity, intent)
        }

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(TAG_PERSON_NAME_INPUT).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_PERSON_NAME_INPUT).assertTextContains(targetName)
    }

    @Test
    fun captureManualFallback_shouldPrefillPersonEditor() {
        composeRule.onNodeWithTag(TAG_BOTTOM_NAV_CAPTURE).performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(TAG_CAPTURE_NAME_INPUT).fetchSemanticsNodes().isNotEmpty()
        }

        val fallbackName = "manual_fallback_name"
        val fallbackId = "110101199001011234"
        composeRule.onNodeWithTag(TAG_CAPTURE_NAME_INPUT).performTextInput(fallbackName)
        composeRule.onNodeWithTag(TAG_CAPTURE_ID_INPUT).performTextInput(fallbackId)
        composeRule.onNodeWithTag(TAG_CAPTURE_APPLY_BUTTON).performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(TAG_PERSON_NAME_INPUT).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_PERSON_NAME_INPUT).assertTextContains(fallbackName)
        composeRule.onNodeWithTag(TAG_PERSON_ID_INPUT).assertTextContains(fallbackId)
        composeRule.onNode(hasText("编辑", substring = true))
    }

    @Test
    fun reminderStatusFlow_shouldTransitionThroughNotificationAndReminderCenter() {
        val personId = 2026021301L
        insertPerson(
            id = personId,
            name = "status_flow_person",
            relation = "family"
        )
        val logId = insertReminderLog(
            personId = personId,
            status = ReminderLogStatus.PLANNED
        )

        sendReminderAlarmBroadcast(
            personId = personId,
            personName = "status_flow_person",
            relation = "family",
            birthday = "1995-06-18",
            offsetDay = 1,
            logId = logId
        )

        composeRule.waitUntil(10_000) {
            getReminderLogStatus(logId) == ReminderLogStatus.SENT
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            val intent = Intent(activity, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_PERSON_ID, personId)
                putExtra(EXTRA_REMINDER_LOG_ID, logId)
            }
            val handleIntentMethod = MainActivity::class.java.getDeclaredMethod("handleIntent", Intent::class.java)
            handleIntentMethod.isAccessible = true
            handleIntentMethod.invoke(activity, intent)
        }

        composeRule.waitUntil(10_000) {
            getReminderLogStatus(logId) == ReminderLogStatus.CLICKED
        }

        composeRule.onNodeWithTag(TAG_BOTTOM_NAV_REMINDER).performClick()

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(reminderMarkDoneTag(logId)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(reminderMarkDoneTag(logId)).performClick()

        composeRule.waitUntil(10_000) {
            getReminderLogStatus(logId) == ReminderLogStatus.DONE
        }
    }

    private fun insertPerson(id: Long, name: String, relation: String) {
        val now = System.currentTimeMillis()
        val person = Person(
            id = id,
            name = name,
            idNumber = null,
            birthdaySolar = LocalDate.of(1995, 6, 18),
            birthdayLunar = null,
            gender = Gender.UNKNOWN,
            relation = relation,
            note = "test_data",
            avatarUri = null,
            reminderConfig = ReminderConfig(
                offsets = listOf(7, 3, 1, 0),
                remindTime = LocalTime.of(9, 0),
                enabled = true
            ),
            createdAt = now,
            updatedAt = now
        )
        runDb {
            runBlocking {
                app().personRepository.upsert(person)
            }
        }
    }

    private fun clearAllPeople() {
        runDb {
            runBlocking {
                val repository = app().personRepository
                val people = repository.observePeople().first()
                people.forEach { person ->
                    repository.softDelete(person.id)
                }
            }
        }
    }

    private fun insertReminderLog(personId: Long, status: ReminderLogStatus): Long {
        val log = ReminderLog(
            personId = personId,
            targetDate = LocalDate.now().plusDays(1),
            offsetDay = 1,
            status = status,
            createdAt = System.currentTimeMillis()
        )
        var savedId = -1L
        runDb {
            runBlocking {
                savedId = app().reminderLogRepository.upsert(log)
            }
        }
        return savedId
    }

    private fun sendReminderAlarmBroadcast(
        personId: Long,
        personName: String,
        relation: String,
        birthday: String,
        offsetDay: Int,
        logId: Long
    ) {
        composeRule.activityRule.scenario.onActivity { activity ->
            val intent = Intent(activity, BirthdayReminderAlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_PERSON_ID, personId)
                putExtra(EXTRA_ALARM_PERSON_NAME, personName)
                putExtra(EXTRA_ALARM_PERSON_RELATION, relation)
                putExtra(EXTRA_ALARM_BIRTHDAY, birthday)
                putExtra(EXTRA_ALARM_OFFSET_DAY, offsetDay)
                putExtra(EXTRA_REMINDER_LOG_ID, logId)
            }
            activity.sendBroadcast(intent)
        }
    }

    private fun getReminderLogStatus(logId: Long): ReminderLogStatus? {
        var status: ReminderLogStatus? = null
        runDb {
            runBlocking {
                status = app().reminderLogRepository.observeLogs().first()
                    .firstOrNull { it.id == logId }
                    ?.status
            }
        }
        return status
    }

    private fun app(): BirthKeeperApplication {
        return composeRule.activity.application as BirthKeeperApplication
    }

    private fun runDb(block: () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit(block).get(20, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }
}
