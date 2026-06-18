package com.lingdict.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.BuildConfig
import com.lingdict.app.data.datastore.UserSettings
import com.lingdict.app.data.remote.DatamuseApiService
import com.lingdict.app.data.remote.FreeDictionaryApiService
import com.lingdict.app.data.remote.MerriamApiService
import com.lingdict.app.data.remote.PexelsApiService
import com.lingdict.app.data.remote.WordsApiService
import com.lingdict.app.data.remote.YoudaoApiService
import com.lingdict.app.domain.repository.SettingsRepository
import com.lingdict.app.domain.usecase.ExportVocabularyPdfUseCase
import com.lingdict.app.util.YoudaoSignUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isExportingVocabulary: Boolean = false,
    val exportError: String? = null,
    val isTestingApis: Boolean = false,
    val apiTestResult: String? = null
)

data class ApiSettingsTestRequest(
    val youdaoEnabled: Boolean,
    val youdaoAppKey: String,
    val youdaoAppSecret: String,
    val freeDictionaryEnabled: Boolean,
    val datamuseEnabled: Boolean,
    val merriamEnabled: Boolean,
    val merriamApiKey: String,
    val wordsApiEnabled: Boolean,
    val wordsApiKey: String,
    val wordsApiHost: String,
    val pexelsApiKey: String
)

sealed class SettingsEffect {
    data class SharePdf(val file: File) : SettingsEffect()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportVocabularyPdfUseCase: ExportVocabularyPdfUseCase,
    private val youdaoApi: YoudaoApiService,
    private val freeDictionaryApi: FreeDictionaryApiService,
    private val datamuseApi: DatamuseApiService,
    private val merriamApi: MerriamApiService,
    private val wordsApi: WordsApiService,
    private val pexelsApi: PexelsApiService
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


    fun testApiSettings(request: ApiSettingsTestRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingApis = true, apiTestResult = null) }
            val results = mutableListOf<String>()

            suspend fun check(name: String, enabled: Boolean = true, block: suspend () -> Boolean) {
                if (!enabled) {
                    results += "$name：未启用"
                    return
                }
                val passed = runCatching { block() }.getOrDefault(false)
                results += "$name：${if (passed) "可用" else "不可用"}"
            }

            check("有道", request.youdaoEnabled) {
                val appKey = request.youdaoAppKey.ifBlank { BuildConfig.YOUDAO_APP_KEY }
                val appSecret = request.youdaoAppSecret.ifBlank { BuildConfig.YOUDAO_APP_SECRET }
                if (appKey.isBlank() || appSecret.isBlank()) return@check false
                val query = "apple"
                val salt = YoudaoSignUtil.generateSalt()
                val curtime = YoudaoSignUtil.getCurrentTime()
                val sign = YoudaoSignUtil.generateSign(appKey, appSecret, query, salt, curtime)
                val response = youdaoApi.translate(
                    query = query,
                    appKey = appKey,
                    salt = salt,
                    sign = sign,
                    curtime = curtime
                )
                response.errorCode == "0" && (!response.translation.isNullOrEmpty() || response.basic != null)
            }

            check("Free Dictionary", request.freeDictionaryEnabled) {
                freeDictionaryApi.lookup("apple").isNotEmpty()
            }

            check("Datamuse", request.datamuseEnabled) {
                datamuseApi.words(spelling = "apple", metadata = "dps", max = 1).isNotEmpty()
            }

            check("Merriam-Webster", request.merriamEnabled) {
                val apiKey = request.merriamApiKey.trim()
                apiKey.isNotBlank() && merriamApi.lookup("apple", apiKey).any { !it.shortDefinitions.isNullOrEmpty() }
            }

            check("WordsAPI", request.wordsApiEnabled) {
                val apiKey = request.wordsApiKey.trim()
                val host = request.wordsApiHost.trim().ifBlank { WordsApiService.DEFAULT_HOST }
                apiKey.isNotBlank() && !wordsApi.lookup("apple", apiKey, host).results.isNullOrEmpty()
            }

            check("Pexels") {
                val apiKey = request.pexelsApiKey.ifBlank { BuildConfig.PEXELS_API_KEY }
                apiKey.isNotBlank() && pexelsApi.searchPhotos(query = "apple", perPage = 1, apiKey = apiKey).photos.isNotEmpty()
            }

            _uiState.update {
                it.copy(
                    isTestingApis = false,
                    apiTestResult = results.joinToString("\n")
                )
            }
        }
    }

    fun clearApiTestResult() {
        _uiState.update { it.copy(apiTestResult = null) }
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
