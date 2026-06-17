package com.lingdict.app.presentation.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.usecase.GetDueWordsUseCase
import com.lingdict.app.domain.usecase.UpdateReviewUseCase
import com.lingdict.app.util.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnUiState(
    val currentWord: UserWord? = null,
    val remainingWords: List<UserWord> = emptyList(),
    val isFlipped: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalWords: Int = 0,
    val currentIndex: Int = 0
)

sealed class LearnEvent {
    object FlipCard : LearnEvent()
    data class SwipeLeft(val quality: Int = 1) : LearnEvent()  // 不认识（修正为 1，确保路由到 markAsUnknown）
    data class SwipeRight(val quality: Int = 4) : LearnEvent() // 认识
    data class SwipeUp(val quality: Int = 5) : LearnEvent()    // 很熟悉
    object PlayAudio : LearnEvent()
    object ClearError : LearnEvent()
}

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val updateReviewUseCase: UpdateReviewUseCase,
    private val ttsManager: TTSManager
) : ViewModel() {

    private val _isFlipped = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _currentIndex = MutableStateFlow(0)

    private val _words = MutableStateFlow<List<UserWord>>(emptyList())

    val uiState: StateFlow<LearnUiState> = combine(
        _words,
        _currentIndex,
        _isFlipped,
        _isLoading,
        _error
    ) { words, index, flipped, loading, error ->
        LearnUiState(
            currentWord = words.getOrNull(index),
            remainingWords = words.drop(index + 1),
            isFlipped = flipped,
            isLoading = loading,
            error = error,
            totalWords = words.size,
            currentIndex = index
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LearnUiState()
    )

    init {
        loadDueWords()
    }

    private fun loadDueWords() {
        viewModelScope.launch {
            _isLoading.value = true
            getDueWordsUseCase(limit = 20)
                .catch { exception ->
                    _error.value = exception.message ?: "加载失败"
                }
                .collect { words ->
                    _words.value = words
                    _currentIndex.value = 0
                    _isLoading.value = false
                }
        }
    }

    fun onEvent(event: LearnEvent) {
        when (event) {
            is LearnEvent.FlipCard -> {
                _isFlipped.value = !_isFlipped.value
            }

            is LearnEvent.SwipeLeft -> {
                handleSwipe(event.quality)
            }

            is LearnEvent.SwipeRight -> {
                handleSwipe(event.quality)
            }

            is LearnEvent.SwipeUp -> {
                handleSwipe(event.quality)
            }

            is LearnEvent.PlayAudio -> {
                val currentWord = currentWord()
                if (currentWord != null && ttsManager.isAvailable()) {
                    ttsManager.speak(currentWord.word.word)
                } else if (!ttsManager.isAvailable()) {
                    _error.value = "语音功能不可用，请检查TTS设置"
                }
            }

            is LearnEvent.ClearError -> {
                _error.value = null
            }
        }
    }

    private fun handleSwipe(quality: Int) {
        val currentWord = currentWord() ?: return

        viewModelScope.launch {
            _isLoading.value = true

            updateReviewUseCase(currentWord.id, quality)
                .onSuccess {
                    // Move to next word
                    _isFlipped.value = false
                    if (_currentIndex.value < _words.value.size - 1) {
                        _currentIndex.value++
                    } else {
                        // All words reviewed, reload
                        loadDueWords()
                    }
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "更新失败"
                }

            _isLoading.value = false
        }
    }

    private fun currentWord(): UserWord? {
        return _words.value.getOrNull(_currentIndex.value)
    }

    fun resetProgress() {
        _currentIndex.value = 0
        _isFlipped.value = false
    }
}
