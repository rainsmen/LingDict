package com.lingdict.app.data.repository

import com.lingdict.app.data.datastore.SettingsDataStore
import com.lingdict.app.data.datastore.UserSettings
import com.lingdict.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun getUserSettings(): Flow<UserSettings> {
        return settingsDataStore.userSettingsFlow
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        settingsDataStore.updateDarkMode(enabled)
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        settingsDataStore.updateNotifications(enabled)
    }

    override suspend fun updateDailyLearningGoal(goal: Int) {
        settingsDataStore.updateDailyLearningGoal(goal)
    }

    override suspend fun updateDailyReviewGoal(goal: Int) {
        settingsDataStore.updateDailyReviewGoal(goal)
    }

    override suspend fun updateAutoPlayAudio(enabled: Boolean) {
        settingsDataStore.updateAutoPlayAudio(enabled)
    }

    override suspend fun updateShowPhonetic(enabled: Boolean) {
        settingsDataStore.updateShowPhonetic(enabled)
    }

    override suspend fun updateCardBackground(enabled: Boolean) {
        settingsDataStore.updateCardBackground(enabled)
    }

    override suspend fun updateYoudaoEnabled(enabled: Boolean) {
        settingsDataStore.updateYoudaoEnabled(enabled)
    }

    override suspend fun updateYoudaoCredentials(appKey: String, appSecret: String) {
        settingsDataStore.updateYoudaoCredentials(appKey, appSecret)
    }

    override suspend fun updatePexelsApiKey(apiKey: String) {
        settingsDataStore.updatePexelsApiKey(apiKey)
    }

    override suspend fun updateOnlineLookupPreferred(enabled: Boolean) {
        settingsDataStore.updateOnlineLookupPreferred(enabled)
    }

    override suspend fun updateFreeDictionaryEnabled(enabled: Boolean) {
        settingsDataStore.updateFreeDictionaryEnabled(enabled)
    }

    override suspend fun updateDatamuseEnabled(enabled: Boolean) {
        settingsDataStore.updateDatamuseEnabled(enabled)
    }

    override suspend fun updateMerriamSettings(enabled: Boolean, apiKey: String) {
        settingsDataStore.updateMerriamSettings(enabled, apiKey)
    }

    override suspend fun updateWordsApiSettings(enabled: Boolean, apiKey: String, host: String) {
        settingsDataStore.updateWordsApiSettings(enabled, apiKey, host)
    }
}
