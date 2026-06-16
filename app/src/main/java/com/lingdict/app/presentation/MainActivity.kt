package com.lingdict.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingdict.app.data.datastore.UserSettings
import com.lingdict.app.domain.repository.SettingsRepository
import com.lingdict.app.presentation.theme.LingDictTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userSettings = settingsRepository.getUserSettings()
                .collectAsStateWithLifecycle(initialValue = UserSettings())

            LingDictTheme(darkTheme = userSettings.value.darkMode) {
                RootNavigation()
            }
        }
    }
}
