package com.lingdict.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.data.datastore.UserSettings
import com.lingdict.app.domain.repository.SettingsRepository
import com.lingdict.app.domain.usecase.ExportVocabularyPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isExportingVocabulary: Boolean = false,
    val exportError: String? = null
)

sealed class SettingsEffect {
    data class SharePdf(val file: File) : SettingsEffect()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportVocabularyPdfUseCase: ExportVocabularyPdfUseCase
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = settingsRepository.getUserSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>()
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    fun exportVocabularyPdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingVocabulary = true, exportError = null) }
            exportVocabularyPdfUseCase()
                .onSuccess { file ->
                    _effects.emit(SettingsEffect.SharePdf(file))
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(exportError = exception.message ?: "导出失败")
                    }
                }
            _uiState.update { it.copy(isExportingVocabulary = false) }
        }
    }

    fun clearExportError() {
        _uiState.update { it.copy(exportError = null) }
    }

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

    fun updateYoudaoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateYoudaoEnabled(enabled)
        }
    }

    fun updateYoudaoCredentials(appKey: String, appSecret: String) {
        viewModelScope.launch {
            settingsRepository.updateYoudaoCredentials(appKey, appSecret)
        }
    }

    fun updatePexelsApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updatePexelsApiKey(apiKey)
        }
    }

    fun updateOnlineLookupPreferred(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateOnlineLookupPreferred(enabled)
        }
    }

    fun updateFreeDictionaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFreeDictionaryEnabled(enabled)
        }
    }

    fun updateDatamuseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDatamuseEnabled(enabled)
        }
    }

    fun updateMerriamSettings(enabled: Boolean, apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateMerriamSettings(enabled, apiKey)
        }
    }

    fun updateWordsApiSettings(enabled: Boolean, apiKey: String, host: String) {
        viewModelScope.launch {
            settingsRepository.updateWordsApiSettings(enabled, apiKey, host)
        }
    }
}
