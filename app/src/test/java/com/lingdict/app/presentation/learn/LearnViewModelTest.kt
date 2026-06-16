package com.lingdict.app.presentation.learn

import app.cash.turbine.test
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.WordStatus
import com.lingdict.app.domain.usecase.GetDueWordsUseCase
import com.lingdict.app.domain.usecase.UpdateReviewUseCase
import com.lingdict.app.presentation.home.MainDispatcherRule
import com.lingdict.app.util.TTSManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearnViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: LearnViewModel
    private lateinit var getDueWordsUseCase: GetDueWordsUseCase
    private lateinit var updateReviewUseCase: UpdateReviewUseCase
    private lateinit var ttsManager: TTSManager

    private val mockWords = listOf(
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
        ),
        UserWord(
            id = 2,
            word = "example",
            phonetic = "/ɪɡˈzɑːmpl/",
            definition = "A thing characteristic",
            translation = "n. 例子",
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

    @Before
    fun setup() {
        getDueWordsUseCase = mockk()
        updateReviewUseCase = mockk()
        ttsManager = mockk(relaxed = true)

        every { getDueWordsUseCase(20) } returns flowOf(mockWords)

        viewModel = LearnViewModel(
            getDueWordsUseCase,
            updateReviewUseCase,
            ttsManager
        )
    }

    @Test
    fun `initial state should load due words`() = runTest {
        // When
        viewModel.uiState.test {
            val state = awaitItem()

            // Then
            assertNotNull(state.currentWord)
            assertEquals("test", state.currentWord?.word)
            assertEquals(2, state.totalWords)
            assertEquals(0, state.currentIndex)
            assertFalse(state.isFlipped)
        }
    }

    @Test
    fun `flipCard should toggle isFlipped state`() = runTest {
        // Given - initial state
        viewModel.uiState.test {
            var state = awaitItem()
            assertFalse(state.isFlipped)

            // When
            viewModel.onEvent(LearnEvent.FlipCard)
            state = awaitItem()

            // Then
            assertTrue(state.isFlipped)

            // When - flip again
            viewModel.onEvent(LearnEvent.FlipCard)
            state = awaitItem()

            // Then
            assertFalse(state.isFlipped)
        }
    }

    @Test
    fun `swipeRight should update review with quality 4`() = runTest {
        // Given
        coEvery { updateReviewUseCase(1, 4) } returns Result.success(Unit)

        // When
        viewModel.onEvent(LearnEvent.SwipeRight())
        advanceUntilIdle()

        // Then
        coVerify { updateReviewUseCase(1, 4) }
    }

    @Test
    fun `swipeLeft should update review with quality 2`() = runTest {
        // Given
        coEvery { updateReviewUseCase(1, 2) } returns Result.success(Unit)

        // When
        viewModel.onEvent(LearnEvent.SwipeLeft())
        advanceUntilIdle()

        // Then
        coVerify { updateReviewUseCase(1, 2) }
    }

    @Test
    fun `swipeUp should update review with quality 5`() = runTest {
        // Given
        coEvery { updateReviewUseCase(1, 5) } returns Result.success(Unit)

        // When
        viewModel.onEvent(LearnEvent.SwipeUp())
        advanceUntilIdle()

        // Then
        coVerify { updateReviewUseCase(1, 5) }
    }

    @Test
    fun `swipe should move to next word`() = runTest {
        // Given
        coEvery { updateReviewUseCase(any(), any()) } returns Result.success(Unit)

        // When
        viewModel.onEvent(LearnEvent.SwipeRight())
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("example", state.currentWord?.word)
            assertEquals(1, state.currentIndex)
            assertFalse(state.isFlipped) // Should reset flip state
        }
    }

    @Test
    fun `swipe on last word should reload words`() = runTest {
        // Given
        coEvery { updateReviewUseCase(any(), any()) } returns Result.success(Unit)
        every { getDueWordsUseCase(20) } returns flowOf(mockWords)

        // When - swipe through all words
        viewModel.onEvent(LearnEvent.SwipeRight()) // First word
        advanceUntilIdle()
        viewModel.onEvent(LearnEvent.SwipeRight()) // Second word
        advanceUntilIdle()

        // Then - should reload and start from beginning
        coVerify(exactly = 2) { getDueWordsUseCase(20) }
    }

    @Test
    fun `playAudio should speak current word when TTS available`() = runTest {
        // Given
        every { ttsManager.isAvailable() } returns true

        // When
        viewModel.onEvent(LearnEvent.PlayAudio)

        // Then
        coVerify { ttsManager.speak("test") }
    }

    @Test
    fun `playAudio should set error when TTS unavailable`() = runTest {
        // Given
        every { ttsManager.isAvailable() } returns false

        // When
        viewModel.onEvent(LearnEvent.PlayAudio)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("语音功能不可用，请检查TTS设置", state.error)
        }
    }

    @Test
    fun `updateReview failure should set error`() = runTest {
        // Given
        coEvery { updateReviewUseCase(any(), any()) } returns Result.failure(
            Exception("Update failed")
        )

        // When
        viewModel.onEvent(LearnEvent.SwipeRight())
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Update failed", state.error)
        }
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given - set an error first
        coEvery { updateReviewUseCase(any(), any()) } returns Result.failure(Exception("Error"))
        viewModel.onEvent(LearnEvent.SwipeRight())
        advanceUntilIdle()

        // When
        viewModel.onEvent(LearnEvent.ClearError)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    @Test
    fun `resetProgress should reset to first word`() = runTest {
        // Given - move to second word
        coEvery { updateReviewUseCase(any(), any()) } returns Result.success(Unit)
        viewModel.onEvent(LearnEvent.SwipeRight())
        advanceUntilIdle()

        // When
        viewModel.resetProgress()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.currentIndex)
            assertEquals("test", state.currentWord?.word)
            assertFalse(state.isFlipped)
        }
    }

    @Test
    fun `empty word list should show no current word`() = runTest {
        // Given
        every { getDueWordsUseCase(20) } returns flowOf(emptyList())

        // When
        val emptyViewModel = LearnViewModel(
            getDueWordsUseCase,
            updateReviewUseCase,
            ttsManager
        )

        // Then
        emptyViewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.currentWord)
            assertEquals(0, state.totalWords)
        }
    }
}
