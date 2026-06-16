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
}
