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
}
