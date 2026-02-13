package com.zhongxul.birthkeeper.feature.reminder

import com.zhongxul.birthkeeper.core.domain.model.Gender
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderConfig
import com.zhongxul.birthkeeper.core.domain.model.ReminderLog
import com.zhongxul.birthkeeper.core.domain.model.ReminderLogStatus
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository
import com.zhongxul.birthkeeper.core.domain.repository.ReminderLogRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `标记已处理后应从待处理移动到已处理`() = runTest {
        val peopleRepo = FakePersonRepository(
            listOf(
                createPerson(id = 1L, name = "张三")
            )
        )
        val logsRepo = FakeReminderLogRepository(
            listOf(
                ReminderLog(
                    id = 101L,
                    personId = 1L,
                    targetDate = LocalDate.now(),
                    offsetDay = 0,
                    status = ReminderLogStatus.SENT,
                    createdAt = System.currentTimeMillis()
                )
            )
        )
        val viewModel = ReminderViewModel(peopleRepo, logsRepo)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.pendingLogs.size)
        assertEquals(0, viewModel.uiState.value.doneLogs.size)

        viewModel.onEvent(ReminderEvent.MarkDoneClicked(101L))
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.pendingLogs.size)
        assertEquals(1, viewModel.uiState.value.doneLogs.size)
    }

    private fun createPerson(id: Long, name: String): Person {
        val now = System.currentTimeMillis()
        return Person(
            id = id,
            name = name,
            idNumber = null,
            birthdaySolar = LocalDate.of(1995, 1, 1),
            birthdayLunar = null,
            gender = Gender.UNKNOWN,
            relation = "朋友",
            note = null,
            avatarUri = null,
            reminderConfig = ReminderConfig(),
            createdAt = now,
            updatedAt = now
        )
    }
}

private class FakePersonRepository(
    initialPeople: List<Person>
) : PersonRepository {
    private val peopleFlow = MutableStateFlow(initialPeople)

    override fun observePeople(): Flow<List<Person>> = peopleFlow.asStateFlow()

    override suspend fun getPersonById(id: Long): Person? {
        return peopleFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun upsert(person: Person): Long {
        val people = peopleFlow.value.toMutableList()
        val index = people.indexOfFirst { it.id == person.id }
        if (index >= 0) {
            people[index] = person
            peopleFlow.value = people
            return person.id
        }
        val id = (people.maxOfOrNull { it.id } ?: 0L) + 1
        people.add(person.copy(id = id))
        peopleFlow.value = people
        return id
    }

    override suspend fun softDelete(id: Long) {
        peopleFlow.value = peopleFlow.value.filterNot { it.id == id }
    }
}

private class FakeReminderLogRepository(
    initialLogs: List<ReminderLog>
) : ReminderLogRepository {
    private val logsFlow = MutableStateFlow(initialLogs)

    override fun observeLogs(): Flow<List<ReminderLog>> = logsFlow.asStateFlow()

    override suspend fun findByKey(
        personId: Long,
        targetDate: LocalDate,
        offsetDay: Int
    ): ReminderLog? {
        return logsFlow.value.firstOrNull {
            it.personId == personId &&
                it.targetDate == targetDate &&
                it.offsetDay == offsetDay
        }
    }

    override suspend fun upsert(log: ReminderLog): Long {
        val logs = logsFlow.value.toMutableList()
        val index = logs.indexOfFirst { it.id == log.id && log.id != 0L }
        if (index >= 0) {
            logs[index] = log
            logsFlow.value = logs
            return log.id
        }
        val id = (logs.maxOfOrNull { it.id } ?: 0L) + 1
        logs.add(log.copy(id = id))
        logsFlow.value = logs
        return id
    }

    override suspend fun updateStatus(logId: Long, status: ReminderLogStatus) {
        val logs = logsFlow.value.toMutableList()
        val index = logs.indexOfFirst { it.id == logId }
        if (index >= 0) {
            logs[index] = logs[index].copy(status = status)
            logsFlow.value = logs
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
