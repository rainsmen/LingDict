package com.lingdict.app.presentation.learn

import app.cash.turbine.test
import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.usecase.GetDueWordsUseCase
import com.lingdict.app.domain.usecase.UpdateReviewUseCase
import com.lingdict.app.presentation.home.MainDispatcherRule
import com.lingdict.app.util.TTSManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
            word = Word(word = "test", phonetic = "/test/", translation = "测试"),
            addedDate = 1L,
            nextReviewDate = 1L,
            status = WordStatus.LEARNING
        ),
        UserWord(
            id = 2,
            word = Word(word = "example", phonetic = "/example/", translation = "例子"),
            addedDate = 1L,
            nextReviewDate = 1L,
            status = WordStatus.LEARNING
        )
    )

    @Before
    fun setup() {
        getDueWordsUseCase = mockk()
        updateReviewUseCase = mockk()
        ttsManager = mockk(relaxed = true)

        every { getDueWordsUseCase(20) } returns flowOf(mockWords)
        coEvery { updateReviewUseCase(any(), any()) } returns Result.success(Unit)

        viewModel = LearnViewModel(getDueWordsUseCase, updateReviewUseCase, ttsManager)
    }

    @Test
    fun `initial state loads due words`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("test", state.currentWord?.word?.word)
            assertEquals(2, state.totalWords)
            assertEquals(0, state.currentIndex)
        }
    }

    @Test
    fun `flip card toggles state`() = runTest {
        viewModel.onEvent(LearnEvent.FlipCard)

        viewModel.uiState.test {
            assertTrue(awaitItem().isFlipped)
        }
    }

    @Test
    fun `swipe right updates review and advances`() = runTest {
        viewModel.onEvent(LearnEvent.SwipeRight())
        advanceUntilIdle()

        coVerify { updateReviewUseCase(1, 4) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("example", state.currentWord?.word?.word)
            assertEquals(1, state.currentIndex)
        }
    }

    @Test
    fun `swipe left uses unknown quality`() = runTest {
        viewModel.onEvent(LearnEvent.SwipeLeft())
        advanceUntilIdle()

        coVerify { updateReviewUseCase(1, 1) }
    }

    @Test
    fun `play audio speaks current word when available`() = runTest {
        every { ttsManager.isAvailable() } returns true
        advanceUntilIdle()

        viewModel.onEvent(LearnEvent.PlayAudio)

        verify { ttsManager.speak("test") }
    }
}
