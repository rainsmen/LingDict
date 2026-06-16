package com.lingdict.app.presentation.home

import app.cash.turbine.test
import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.usecase.AddUserWordUseCase
import com.lingdict.app.domain.usecase.GetDueWordsUseCase
import com.lingdict.app.domain.usecase.GetTodayProgressUseCase
import com.lingdict.app.domain.usecase.SearchWordUseCase
import com.lingdict.app.domain.usecase.TodayProgress
import com.lingdict.app.util.TTSManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel
    private lateinit var searchWordUseCase: SearchWordUseCase
    private lateinit var getDueWordsUseCase: GetDueWordsUseCase
    private lateinit var addUserWordUseCase: AddUserWordUseCase
    private lateinit var getTodayProgressUseCase: GetTodayProgressUseCase
    private lateinit var ttsManager: TTSManager

    @Before
    fun setup() {
        searchWordUseCase = mockk()
        getDueWordsUseCase = mockk()
        addUserWordUseCase = mockk()
        getTodayProgressUseCase = mockk()
        ttsManager = mockk(relaxed = true)

        every { getDueWordsUseCase(any()) } returns flowOf(emptyList())
        every { getTodayProgressUseCase() } returns flowOf(TodayProgress())
        every { searchWordUseCase(any()) } returns flowOf(emptyList())

        viewModel = HomeViewModel(
            searchWordUseCase,
            getDueWordsUseCase,
            addUserWordUseCase,
            getTodayProgressUseCase,
            ttsManager
        )
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
            assertTrue(state.dueWords.isEmpty())
            assertEquals(0, state.todayLearned)
            assertEquals(0, state.todayReviewed)
        }
    }

    @Test
    fun `search query updates and returns words`() = runTest {
        every { searchWordUseCase("dict") } returns flowOf(
            listOf(Word(word = "dictionary", translation = "字典", level = "CET4"))
        )

        viewModel.onEvent(HomeEvent.SearchQueryChanged("dict"))
        advanceTimeBy(400)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("dict", state.searchQuery)
            assertEquals("dictionary", state.searchResults.first().word)
        }
    }

    @Test
    fun `due words are exposed`() = runTest {
        every { getDueWordsUseCase(5) } returns flowOf(
            listOf(
                UserWord(
                    id = 1,
                    word = Word(word = "test", translation = "测试"),
                    addedDate = 1L,
                    nextReviewDate = 1L,
                    status = WordStatus.LEARNING
                )
            )
        )

        viewModel = HomeViewModel(
            searchWordUseCase,
            getDueWordsUseCase,
            addUserWordUseCase,
            getTodayProgressUseCase,
            ttsManager
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("test", state.dueWords.first().word.word)
        }
    }

    @Test
    fun `add to library calls use case`() = runTest {
        coEvery { addUserWordUseCase("test") } returns Result.success(Unit)

        viewModel.onEvent(HomeEvent.AddToLibrary("test"))
        advanceUntilIdle()

        coVerify { addUserWordUseCase("test") }
    }
}
