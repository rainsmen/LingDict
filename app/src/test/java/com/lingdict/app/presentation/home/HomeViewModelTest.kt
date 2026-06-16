package com.lingdict.app.presentation.home

import app.cash.turbine.test
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.model.WordStatus
import com.lingdict.app.domain.usecase.*
import com.lingdict.app.util.TTSManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
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

        // Default mock behaviors
        every { getDueWordsUseCase(any()) } returns flowOf(emptyList())
        every { getTodayProgressUseCase() } returns flowOf(TodayProgress())

        viewModel = HomeViewModel(
            searchWordUseCase,
            getDueWordsUseCase,
            addUserWordUseCase,
            getTodayProgressUseCase,
            ttsManager
        )
    }

    @After
    fun tearDown() {
        // Clean up if needed
    }

    @Test
    fun `initial state should be empty`() = runTest {
        // When - observe initial state
        viewModel.uiState.test {
            val state = awaitItem()

            // Then
            assertEquals("", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
            assertTrue(state.dueWords.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(0, state.todayLearned)
            assertEquals(0, state.todayReviewed)
        }
    }

    @Test
    fun `search query change should update state`() = runTest {
        // Given
        every { searchWordUseCase(any()) } returns flowOf(emptyList())

        // When
        viewModel.onEvent(HomeEvent.SearchQueryChanged("test"))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("test", state.searchQuery)
        }
    }

    @Test
    fun `search with query length less than 2 should not trigger search`() = runTest {
        // Given
        every { searchWordUseCase(any()) } returns flowOf(emptyList())

        // When
        viewModel.onEvent(HomeEvent.SearchQueryChanged("a"))
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { searchWordUseCase(any()) }
    }

    @Test
    fun `search with valid query should return results`() = runTest {
        // Given
        val mockWords = listOf(
            Word("dictionary", "/ˈdɪkʃəneri/", "A book...", "n. 字典", "CET4")
        )
        every { searchWordUseCase("dict") } returns flowOf(mockWords)

        // When
        viewModel.onEvent(HomeEvent.SearchQueryChanged("dict"))
        advanceTimeBy(400) // Wait for debounce

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.searchResults.size)
            assertEquals("dictionary", state.searchResults[0].word)
        }
    }

    @Test
    fun `getDueWords should load due words`() = runTest {
        // Given
        val mockDueWords = listOf(
            UserWord(
                id = 1,
                word = "test",
                phonetic = "/test/",
                definition = "A procedure",
                translation = "n. 测试",
                level = "CET4",
                addedDate = System.currentTimeMillis(),
                lastReviewDate = null,
                nextReviewDate = System.currentTimeMillis(),
                easeFactor = 2.5f,
                interval = 1,
                repetitions = 0,
                status = WordStatus.LEARNING
            )
        )
        every { getDueWordsUseCase(5) } returns flowOf(mockDueWords)

        // Re-create viewModel to trigger init
        viewModel = HomeViewModel(
            searchWordUseCase,
            getDueWordsUseCase,
            addUserWordUseCase,
            getTodayProgressUseCase,
            ttsManager
        )

        // When
        viewModel.uiState.test {
            val state = awaitItem()

            // Then
            assertEquals(1, state.dueWords.size)
            assertEquals("test", state.dueWords[0].word)
        }
    }

    @Test
    fun `getTodayProgress should load today's stats`() = runTest {
        // Given
        every { getTodayProgressUseCase() } returns flowOf(
            TodayProgress(wordsLearned = 5, wordsReviewed = 10)
        )

        // Re-create viewModel to trigger init
        viewModel = HomeViewModel(
            searchWordUseCase,
            getDueWordsUseCase,
            addUserWordUseCase,
            getTodayProgressUseCase,
            ttsManager
        )

        // When
        viewModel.uiState.test {
            val state = awaitItem()

            // Then
            assertEquals(5, state.todayLearned)
            assertEquals(10, state.todayReviewed)
        }
    }

    @Test
    fun `addToLibrary should call addUserWordUseCase`() = runTest {
        // Given
        coEvery { addUserWordUseCase("test") } returns Result.success(Unit)

        // When
        viewModel.onEvent(HomeEvent.AddToLibrary("test"))
        advanceUntilIdle()

        // Then
        coVerify { addUserWordUseCase("test") }
    }

    @Test
    fun `addToLibrary failure should set error`() = runTest {
        // Given
        coEvery { addUserWordUseCase("test") } returns Result.failure(
            Exception("Failed to add")
        )

        // When
        viewModel.onEvent(HomeEvent.AddToLibrary("test"))
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Failed to add", state.error)
        }
    }

    @Test
    fun `playAudio should call ttsManager when available`() = runTest {
        // Given
        every { ttsManager.isAvailable() } returns true

        // When
        viewModel.onEvent(HomeEvent.PlayAudio("test"))

        // Then
        coVerify { ttsManager.speak("test") }
    }

    @Test
    fun `playAudio should set error when TTS unavailable`() = runTest {
        // Given
        every { ttsManager.isAvailable() } returns false

        // When
        viewModel.onEvent(HomeEvent.PlayAudio("test"))
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("语音功能不可用", state.error)
        }
    }

    @Test
    fun `wordSelected should emit navigation event`() = runTest {
        // When
        viewModel.navigationEvent.test {
            viewModel.onEvent(HomeEvent.WordSelected("dictionary"))

            // Then
            val event = awaitItem()
            assertTrue(event is HomeEffect.NavigateToWordDetail)
            assertEquals("dictionary", (event as HomeEffect.NavigateToWordDetail).word)
        }
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given - set an error first
        coEvery { addUserWordUseCase("test") } returns Result.failure(Exception("Error"))
        viewModel.onEvent(HomeEvent.AddToLibrary("test"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(HomeEvent.ClearError)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }
}
