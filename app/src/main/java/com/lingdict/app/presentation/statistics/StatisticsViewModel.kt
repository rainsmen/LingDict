package com.lingdict.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.model.StudyStatistics
import com.lingdict.app.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StatisticsUiState(
    val statistics: StudyStatistics? = null,
    val dailyRecords: List<DailyRecord> = emptyList(),
    val learningStreak: Int = 0,
    val totalWordsLearned: Int = 0,
    val masteredWords: Int = 0,
    val newWords: Int = 0,
    val learningWords: Int = 0,
    val wordDistribution: Map<WordStatus, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPeriod: TimePeriod = TimePeriod.WEEK
)

data class DailyRecord(
    val date: LocalDate,
    val wordsLearned: Int,
    val wordsReviewed: Int,
    val testsCompleted: Int,
    val accuracy: Float
)

enum class TimePeriod {
    WEEK,
    MONTH,
    YEAR
}

sealed class StatisticsEvent {
    data class SelectPeriod(val period: TimePeriod) : StatisticsEvent()
    object Refresh : StatisticsEvent()
    object ClearError : StatisticsEvent()
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.WEEK)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _statistics = _selectedPeriod
        .flatMapLatest { period ->
            val days = when (period) {
                TimePeriod.WEEK -> 7
                TimePeriod.MONTH -> 30
                TimePeriod.YEAR -> 365
            }
            flow<StudyStatistics?> {
                val stats = getStatisticsUseCase(days)
                emit(stats)
            }.catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        _statistics,
        _selectedPeriod,
        _isLoading,
        _error
    ) { stats, period, loading, error ->
        val dailyRecords = stats?.recentTrend?.map { record ->
            DailyRecord(
                date = LocalDate.ofEpochDay(record.date / (24 * 60 * 60 * 1000)),
                wordsLearned = record.wordsLearned,
                wordsReviewed = record.wordsReviewed,
                testsCompleted = record.testTotal,
                accuracy = record.getAccuracy()
            )
        } ?: emptyList()

        StatisticsUiState(
            statistics = stats,
            dailyRecords = dailyRecords,
            learningStreak = stats?.studyStreak ?: 0,
            totalWordsLearned = stats?.totalWordsLearned ?: 0,
            masteredWords = stats?.masteryDistribution?.masteredWords ?: 0,
            newWords = stats?.masteryDistribution?.newWords ?: 0,
            learningWords = stats?.masteryDistribution?.learningWords ?: 0,
            wordDistribution = mapOf(
                WordStatus.NEW to (stats?.masteryDistribution?.newWords ?: 0),
                WordStatus.LEARNING to (stats?.masteryDistribution?.learningWords ?: 0),
                WordStatus.MASTERED to (stats?.masteryDistribution?.masteredWords ?: 0)
            ).filterValues { it > 0 },
            isLoading = loading,
            error = error,
            selectedPeriod = period
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatisticsUiState()
    )

    init {
        loadStatistics()
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.SelectPeriod -> {
                _selectedPeriod.value = event.period
            }

            is StatisticsEvent.Refresh -> {
                loadStatistics()
            }

            is StatisticsEvent.ClearError -> {
                _error.value = null
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            // Statistics will be loaded through Flow
            _isLoading.value = false
        }
    }
}

private fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + Math.random().toFloat() * (endInclusive - start)
}
