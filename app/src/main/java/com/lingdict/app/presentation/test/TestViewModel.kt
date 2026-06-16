package com.lingdict.app.presentation.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.domain.model.Question
import com.lingdict.app.domain.usecase.GenerateTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TestType {
    MULTIPLE_CHOICE,    // 选择题
    FILL_IN_BLANK,      // 填空题
    LISTENING,          // 听力题
    TRUE_FALSE          // 判断题
}

data class TestUiState(
    val currentQuestion: Question? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val questions: List<Question> = emptyList(),
    val selectedAnswer: String? = null,
    val isAnswerSubmitted: Boolean = false,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val correctCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val testType: TestType? = null,
    val isTestCompleted: Boolean = false
)

sealed class TestEvent {
    data class SelectTestType(val type: TestType, val questionCount: Int = 10) : TestEvent()
    data class SelectAnswer(val answer: String) : TestEvent()
    object SubmitAnswer : TestEvent()
    object NextQuestion : TestEvent()
    object RestartTest : TestEvent()
    object PlayAudio : TestEvent()
    object ClearError : TestEvent()
}

@HiltViewModel
class TestViewModel @Inject constructor(
    private val generateTestUseCase: GenerateTestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    fun onEvent(event: TestEvent) {
        when (event) {
            is TestEvent.SelectTestType -> {
                startTest(event.type, event.questionCount)
            }

            is TestEvent.SelectAnswer -> {
                if (!_uiState.value.isAnswerSubmitted) {
                    _uiState.update { it.copy(selectedAnswer = event.answer) }
                }
            }

            is TestEvent.SubmitAnswer -> {
                submitAnswer()
            }

            is TestEvent.NextQuestion -> {
                moveToNextQuestion()
            }

            is TestEvent.RestartTest -> {
                _uiState.value = TestUiState()
            }

            is TestEvent.PlayAudio -> {
                // TODO: Implement TTS audio playback
            }

            is TestEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun startTest(type: TestType, count: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            generateTestUseCase(type, count)
                .onSuccess { questions ->
                    _uiState.update {
                        it.copy(
                            testType = type,
                            questions = questions,
                            totalQuestions = questions.size,
                            currentQuestion = questions.firstOrNull(),
                            currentQuestionIndex = 0,
                            isLoading = false,
                            score = 0,
                            correctCount = 0
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "生成测试失败",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun submitAnswer() {
        val state = _uiState.value
        val selectedAnswer = state.selectedAnswer ?: return
        val currentQuestion = state.currentQuestion ?: return

        val isCorrect = when (currentQuestion) {
            is Question.MultipleChoice -> selectedAnswer == currentQuestion.correctAnswer
            is Question.FillInBlank -> selectedAnswer.equals(currentQuestion.correctAnswer, ignoreCase = true)
            is Question.Listening -> selectedAnswer.equals(currentQuestion.correctAnswer, ignoreCase = true)
            is Question.TrueFalse -> selectedAnswer.toBoolean() == currentQuestion.correctAnswer
        }

        val newCorrectCount = if (isCorrect) state.correctCount + 1 else state.correctCount

        _uiState.update {
            it.copy(
                isAnswerSubmitted = true,
                isCorrect = isCorrect,
                correctCount = newCorrectCount
            )
        }
    }

    private fun moveToNextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentQuestionIndex + 1

        if (nextIndex < state.totalQuestions) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    currentQuestion = it.questions[nextIndex],
                    selectedAnswer = null,
                    isAnswerSubmitted = false,
                    isCorrect = null
                )
            }
        } else {
            // Test completed
            val score = (state.correctCount.toFloat() / state.totalQuestions * 100).toInt()
            _uiState.update {
                it.copy(
                    isTestCompleted = true,
                    score = score
                )
            }
        }
    }
}
