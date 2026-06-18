package com.lingdict.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dailyLearningGoal: Int = 20,
    val dailyReviewGoal: Int = 30,
    val autoPlayAudio: Boolean = false,
    val showPhonetic: Boolean = true,
    val cardBackgroundEnabled: Boolean = true,
    val youdaoEnabled: Boolean = true,
    val youdaoAppKey: String = "",
    val youdaoAppSecret: String = "",
    val pexelsApiKey: String = "",
    val freeDictionaryEnabled: Boolean = true,
    val datamuseEnabled: Boolean = true
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DAILY_LEARNING_GOAL = intPreferencesKey("daily_learning_goal")
        val DAILY_REVIEW_GOAL = intPreferencesKey("daily_review_goal")
        val AUTO_PLAY_AUDIO = booleanPreferencesKey("auto_play_audio")
        val SHOW_PHONETIC = booleanPreferencesKey("show_phonetic")
        val CARD_BACKGROUND_ENABLED = booleanPreferencesKey("card_background_enabled")
        val YOUDAO_ENABLED = booleanPreferencesKey("youdao_enabled")
        val YOUDAO_APP_KEY = stringPreferencesKey("youdao_app_key")
        val YOUDAO_APP_SECRET = stringPreferencesKey("youdao_app_secret")
        val PEXELS_API_KEY = stringPreferencesKey("pexels_api_key")
        val FREE_DICTIONARY_ENABLED = booleanPreferencesKey("free_dictionary_enabled")
        val DATAMUSE_ENABLED = booleanPreferencesKey("datamuse_enabled")
    }

    // Read settings as Flow
    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                darkMode = preferences[PreferencesKeys.DARK_MODE] ?: false,
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                dailyLearningGoal = preferences[PreferencesKeys.DAILY_LEARNING_GOAL] ?: 20,
                dailyReviewGoal = preferences[PreferencesKeys.DAILY_REVIEW_GOAL] ?: 30,
                autoPlayAudio = preferences[PreferencesKeys.AUTO_PLAY_AUDIO] ?: false,
                showPhonetic = preferences[PreferencesKeys.SHOW_PHONETIC] ?: true,
                cardBackgroundEnabled = preferences[PreferencesKeys.CARD_BACKGROUND_ENABLED] ?: true,
                youdaoEnabled = preferences[PreferencesKeys.YOUDAO_ENABLED] ?: true,
                youdaoAppKey = preferences[PreferencesKeys.YOUDAO_APP_KEY].orEmpty(),
                youdaoAppSecret = preferences[PreferencesKeys.YOUDAO_APP_SECRET].orEmpty(),
                pexelsApiKey = preferences[PreferencesKeys.PEXELS_API_KEY].orEmpty(),
                freeDictionaryEnabled = preferences[PreferencesKeys.FREE_DICTIONARY_ENABLED] ?: true,
                datamuseEnabled = preferences[PreferencesKeys.DATAMUSE_ENABLED] ?: true
            )
        }

    // Update dark mode
    suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    // Update notifications
    suspend fun updateNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // Update daily learning goal
    suspend fun updateDailyLearningGoal(goal: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_LEARNING_GOAL] = goal.coerceIn(1, 100)
        }
    }

    // Update daily review goal
    suspend fun updateDailyReviewGoal(goal: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REVIEW_GOAL] = goal.coerceIn(1, 200)
        }
    }

    // Update auto play audio
    suspend fun updateAutoPlayAudio(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_PLAY_AUDIO] = enabled
        }
    }

    // Update show phonetic
    suspend fun updateShowPhonetic(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_PHONETIC] = enabled
        }
    }

    // Update card background
    suspend fun updateCardBackground(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CARD_BACKGROUND_ENABLED] = enabled
        }
    }

    suspend fun updateYoudaoEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.YOUDAO_ENABLED] = enabled
        }
    }

    suspend fun updateYoudaoCredentials(appKey: String, appSecret: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.YOUDAO_APP_KEY] = appKey.trim()
            preferences[PreferencesKeys.YOUDAO_APP_SECRET] = appSecret.trim()
        }
    }

    suspend fun updatePexelsApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PEXELS_API_KEY] = apiKey.trim()
        }
    }

    suspend fun updateFreeDictionaryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FREE_DICTIONARY_ENABLED] = enabled
        }
    }

    suspend fun updateDatamuseEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATAMUSE_ENABLED] = enabled
        }
    }

    // Clear all settings
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
