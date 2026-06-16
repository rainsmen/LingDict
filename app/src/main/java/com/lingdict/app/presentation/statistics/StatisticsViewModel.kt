package com.lingdict.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.domain.model.Statistics
import com.lingdict.app.domain.model.WordStatus
import com.lingdict.app.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StatisticsUiState(
    val statistics: Statistics? = null,
    val dailyRecords: List<DailyRecord> = emptyList(),
    val wordDistribution: Map<WordStatus, Int> = emptyMap(),
    val learningStreak: Int = 0,
    val totalWordsLearned: Int = 0,
    val masteredWords: Int = 0,
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
            getStatisticsUseCase(days)
                .catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        _statistics,
        _selectedPeriod,
        _isLoading,
        _error
    ) { stats, period, loading, error ->
        val dailyRecords = generateDailyRecords(stats, period)
        val wordDistribution = stats?.let {
            mapOf(
                WordStatus.NEW to it.newWords,
                WordStatus.LEARNING to it.learningWords,
                WordStatus.MASTERED to it.masteredWords
            )
        } ?: emptyMap()

        StatisticsUiState(
            statistics = stats,
            dailyRecords = dailyRecords,
            wordDistribution = wordDistribution,
            learningStreak = stats?.consecutiveDays ?: 0,
            totalWordsLearned = stats?.totalWords ?: 0,
            masteredWords = stats?.masteredWords ?: 0,
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

    private fun generateDailyRecords(stats: Statistics?, period: TimePeriod): List<DailyRecord> {
        val days = when (period) {
            TimePeriod.WEEK -> 7
            TimePeriod.MONTH -> 30
            TimePeriod.YEAR -> 365
        }

        // Use real data from statistics if available
        return (0 until days).map { dayOffset ->
            val date = LocalDate.now().minusDays(dayOffset.toLong())

            // In a real implementation, you would query StudyRecordRepository for each date
            // For now, we'll use the statistics data if available
            DailyRecord(
                date = date,
                wordsLearned = if (stats != null && dayOffset == 0) stats.newWords else 0,
                wordsReviewed = if (stats != null && dayOffset == 0) stats.learningWords else 0,
                testsCompleted = 0,
                accuracy = 0f
            )
        }.reversed()
    }
}

private fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + Math.random().toFloat() * (endInclusive - start)
}
