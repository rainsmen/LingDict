package com.lingdict.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.usecase.AddUserWordUseCase
import com.lingdict.app.domain.usecase.GetDueWordsUseCase
import com.lingdict.app.domain.usecase.GetTodayProgressUseCase
import com.lingdict.app.domain.usecase.SearchWordUseCase
import com.lingdict.app.util.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val searchQuery: String = "",
    val searchResults: List<Word> = emptyList(),
    val dueWords: List<UserWord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val todayLearned: Int = 0,
    val todayReviewed: Int = 0
)

sealed class HomeEvent {
    data class SearchQueryChanged(val query: String) : HomeEvent()
    data class WordSelected(val word: String) : HomeEvent()
    data class AddToLibrary(val word: String) : HomeEvent()
    data class PlayAudio(val word: String) : HomeEvent()
    object ClearError : HomeEvent()
}

sealed class HomeEffect {
    data class NavigateToWordDetail(val word: String) : HomeEffect()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchWordUseCase: SearchWordUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val addUserWordUseCase: AddUserWordUseCase,
    private val getTodayProgressUseCase: GetTodayProgressUseCase,
    private val ttsManager: TTSManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    private val _navigationEvent = MutableSharedFlow<HomeEffect>()
    val navigationEvent: SharedFlow<HomeEffect> = _navigationEvent.asSharedFlow()

    private val searchResults: StateFlow<List<Word>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length >= 2) {
                searchWordUseCase(query)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val dueWords: StateFlow<List<UserWord>> = getDueWordsUseCase(limit = 5)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todayProgress = getTodayProgressUseCase()
        .catch { emit(com.lingdict.app.domain.usecase.TodayProgress()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.lingdict.app.domain.usecase.TodayProgress())

    private val baseUiState = combine(
        _searchQuery,
        searchResults,
        dueWords,
        _isLoading,
        _error
    ) { query, results, due, loading, error ->
        HomeUiState(
            searchQuery = query,
            searchResults = results,
            dueWords = due,
            isLoading = loading,
            error = error
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseUiState,
        todayProgress
    ) { state, progress ->
        state.copy(
            todayLearned = progress.wordsLearned,
            todayReviewed = progress.wordsReviewed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SearchQueryChanged -> {
                _searchQuery.value = event.query
            }

            is HomeEvent.WordSelected -> {
                viewModelScope.launch {
                    _navigationEvent.emit(HomeEffect.NavigateToWordDetail(event.word))
                }
            }

            is HomeEvent.AddToLibrary -> {
                addWordToLibrary(event.word)
            }

            is HomeEvent.PlayAudio -> {
                if (ttsManager.isAvailable()) {
                    ttsManager.speak(event.word)
                } else {
                    _error.value = "语音功能不可用"
                }
            }

            is HomeEvent.ClearError -> {
                _error.value = null
            }
        }
    }

    private fun addWordToLibrary(word: String) {
        viewModelScope.launch {
            _isLoading.value = true
            addUserWordUseCase(word)
                .onSuccess {
                    // Word added successfully
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "添加失败"
                }
            _isLoading.value = false
        }
    }
}
