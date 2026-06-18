package com.lingdict.app.domain.repository

import com.lingdict.app.data.datastore.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getUserSettings(): Flow<UserSettings>
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateNotifications(enabled: Boolean)
    suspend fun updateDailyLearningGoal(goal: Int)
    suspend fun updateDailyReviewGoal(goal: Int)
    suspend fun updateAutoPlayAudio(enabled: Boolean)
    suspend fun updateShowPhonetic(enabled: Boolean)
    suspend fun updateCardBackground(enabled: Boolean)
    suspend fun updateYoudaoEnabled(enabled: Boolean)
    suspend fun updateYoudaoCredentials(appKey: String, appSecret: String)
    suspend fun updatePexelsApiKey(apiKey: String)
    suspend fun updateOnlineLookupPreferred(enabled: Boolean)
    suspend fun updateFreeDictionaryEnabled(enabled: Boolean)
    suspend fun updateDatamuseEnabled(enabled: Boolean)
    suspend fun updateMerriamSettings(enabled: Boolean, apiKey: String)
    suspend fun updateWordsApiSettings(enabled: Boolean, apiKey: String, host: String)
}
