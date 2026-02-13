package com.zhongxul.birthkeeper.feature.person

import com.zhongxul.birthkeeper.core.domain.model.Gender
import com.zhongxul.birthkeeper.core.domain.model.Person
import com.zhongxul.birthkeeper.core.domain.model.ReminderConfig
import com.zhongxul.birthkeeper.core.domain.repository.PersonRepository
import com.zhongxul.birthkeeper.core.domain.usecase.ParseIdCardUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class PersonListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `搜索和关系筛选应返回正确列表`() = runTest {
        val repository = FakePersonRepository(
            initialPeople = listOf(
                createPerson(id = 1L, name = "张三", relation = "家人", note = "生日在春节前"),
                createPerson(id = 2L, name = "李四", relation = "朋友", note = "同事")
            )
        )
        val viewModel = PersonListViewModel(repository)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.people.size)

        viewModel.onEvent(PersonListEvent.SearchChanged("张"))
        assertEquals(1, viewModel.uiState.value.people.size)
        assertEquals("张三", viewModel.uiState.value.people.first().name)

        viewModel.onEvent(PersonListEvent.RelationFilterChanged("家人"))
        assertEquals(1, viewModel.uiState.value.people.size)
        assertEquals("家人", viewModel.uiState.value.people.first().relation)
    }

    @Test
    fun `解析身份证后应自动回填生日和性别`() = runTest {
        val repository = FakePersonRepository()
        val viewModel = PersonListViewModel(repository, ParseIdCardUseCase())
        advanceUntilIdle()

        viewModel.onEvent(PersonListEvent.AddClicked)
        viewModel.onEvent(PersonListEvent.IdNumberChanged("11010519491231002X"))
        viewModel.onEvent(PersonListEvent.ParseIdCardClicked)

        val editor = viewModel.uiState.value.editor
        requireNotNull(editor)
        assertEquals("1949-12-31", editor.birthday)
        assertEquals(Gender.FEMALE, editor.gender)
        assertTrue((editor.age ?: 0) > 0)
    }

    private fun createPerson(
        id: Long,
        name: String,
        relation: String,
        note: String?
    ): Person {
        val now = System.currentTimeMillis()
        return Person(
            id = id,
            name = name,
            idNumber = null,
            birthdaySolar = LocalDate.of(1990, 1, 1),
            birthdayLunar = null,
            gender = Gender.UNKNOWN,
            relation = relation,
            note = note,
            avatarUri = null,
            reminderConfig = ReminderConfig(),
            createdAt = now,
            updatedAt = now
        )
    }
}

private class FakePersonRepository(
    initialPeople: List<Person> = emptyList()
) : PersonRepository {
    private val peopleFlow = MutableStateFlow(initialPeople)

    override fun observePeople(): Flow<List<Person>> = peopleFlow.asStateFlow()

    override suspend fun getPersonById(id: Long): Person? {
        return peopleFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun upsert(person: Person): Long {
        val current = peopleFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == person.id && person.id != 0L }
        if (index >= 0) {
            current[index] = person
            peopleFlow.value = current
            return person.id
        }
        val newId = ((current.maxOfOrNull { it.id } ?: 0L) + 1L)
        current.add(person.copy(id = newId))
        peopleFlow.value = current
        return newId
    }

    override suspend fun softDelete(id: Long) {
        peopleFlow.value = peopleFlow.value.filterNot { it.id == id }
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
