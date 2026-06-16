package com.lingdict.app.presentation.word

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.repository.PexelsRepository
import com.lingdict.app.domain.usecase.AddUserWordUseCase
import com.lingdict.app.domain.usecase.SearchWordUseCase
import com.lingdict.app.util.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordDetailUiState(
    val word: Word? = null,
    val imageUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAddedToLibrary: Boolean = false
)

sealed class WordDetailEvent {
    object PlayAudio : WordDetailEvent()
    object AddToLibrary : WordDetailEvent()
    object ClearError : WordDetailEvent()
}

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchWordUseCase: SearchWordUseCase,
    private val addUserWordUseCase: AddUserWordUseCase,
    private val pexelsRepository: PexelsRepository,
    private val ttsManager: TTSManager
) : ViewModel() {

    private val wordParam: String = savedStateHandle.get<String>("word") ?: ""

    private val _uiState = MutableStateFlow(WordDetailUiState(isLoading = true))
    val uiState: StateFlow<WordDetailUiState> = _uiState.asStateFlow()

    init {
        loadWordDetail()
    }

    private fun loadWordDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            searchWordUseCase(wordParam)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "加载失败"
                        )
                    }
                }
                .collect { words ->
                    val word = words.firstOrNull()
                    if (word != null) {
                        _uiState.update { it.copy(word = word, isLoading = false) }
                        loadImage(word.word)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "单词不存在"
                            )
                        }
                    }
                }
        }
    }

    private fun loadImage(word: String) {
        viewModelScope.launch {
            pexelsRepository.searchPhotos(word, 1)
                .onSuccess { photos ->
                    _uiState.update {
                        it.copy(imageUrl = photos.firstOrNull()?.src?.medium)
                    }
                }
                .onFailure {
                    // Ignore image loading failure, word details are more important
                }
        }
    }

    fun onEvent(event: WordDetailEvent) {
        when (event) {
            is WordDetailEvent.PlayAudio -> {
                val word = _uiState.value.word
                if (word != null && ttsManager.isAvailable()) {
                    ttsManager.speak(word.word)
                }
            }

            is WordDetailEvent.AddToLibrary -> {
                addToLibrary()
            }

            is WordDetailEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun addToLibrary() {
        val word = _uiState.value.word ?: return

        viewModelScope.launch {
            addUserWordUseCase(word.word)
                .onSuccess {
                    _uiState.update { it.copy(isAddedToLibrary = true) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = exception.message ?: "添加失败")
                    }
                }
        }
    }
}
