package com.lingdict.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.data.datastore.UserSettings
import com.lingdict.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = settingsRepository.getUserSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkMode(enabled)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotifications(enabled)
        }
    }

    fun updateDailyLearningGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository.updateDailyLearningGoal(goal)
        }
    }

    fun updateDailyReviewGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository.updateDailyReviewGoal(goal)
        }
    }

    fun updateAutoPlayAudio(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoPlayAudio(enabled)
        }
    }

    fun updateShowPhonetic(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowPhonetic(enabled)
        }
    }

    fun updateCardBackground(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCardBackground(enabled)
        }
    }
}
