package com.lingdict.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.lingdict.app.data.datastore.UserSettings
import com.lingdict.app.presentation.theme.LingDictTheme

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    SettingsContent(
        userSettings = userSettings,
        onDarkModeChange = viewModel::updateDarkMode,
        onNotificationsChange = viewModel::updateNotifications,
        onDailyLearningGoalChange = viewModel::updateDailyLearningGoal,
        onDailyReviewGoalChange = viewModel::updateDailyReviewGoal,
        onAutoPlayAudioChange = viewModel::updateAutoPlayAudio,
        onShowPhoneticChange = viewModel::updateShowPhonetic,
        onCardBackgroundChange = viewModel::updateCardBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    userSettings: UserSettings,
    onDarkModeChange: (Boolean) -> Unit = {},
    onNotificationsChange: (Boolean) -> Unit = {},
    onDailyLearningGoalChange: (Int) -> Unit = {},
    onDailyReviewGoalChange: (Int) -> Unit = {},
    onAutoPlayAudioChange: (Boolean) -> Unit = {},
    onShowPhoneticChange: (Boolean) -> Unit = {},
    onCardBackgroundChange: (Boolean) -> Unit = {}
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Appearance Section
            SettingsSection(title = "外观") {
                SettingsSwitch(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = "使用深色主题",
                    checked = userSettings.darkMode,
                    onCheckedChange = onDarkModeChange
                )
            }

            Divider()

            // Notifications Section
            SettingsSection(title = "通知") {
                SettingsSwitch(
                    icon = Icons.Default.Notifications,
                    title = "学习提醒",
                    subtitle = "每日学习提醒通知",
                    checked = userSettings.notificationsEnabled,
                    onCheckedChange = onNotificationsChange
                )
            }

            Divider()

            // Study Settings Section
            SettingsSection(title = "学习设置") {
                SettingsItem(
                    icon = Icons.Default.School,
                    title = "每日学习目标",
                    subtitle = "${userSettings.dailyLearningGoal}个新单词",
                    onClick = { /* TODO: Show dialog to change */ }
                )
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "每日复习目标",
                    subtitle = "${userSettings.dailyReviewGoal}个复习单词",
                    onClick = { /* TODO: Show dialog to change */ }
                )
                SettingsSwitch(
                    icon = Icons.Default.VolumeUp,
                    title = "自动播放发音",
                    subtitle = "查看单词时自动播放发音",
                    checked = userSettings.autoPlayAudio,
                    onCheckedChange = onAutoPlayAudioChange
                )
                SettingsSwitch(
                    icon = Icons.Default.TextFields,
                    title = "显示音标",
                    subtitle = "在卡片上显示音标",
                    checked = userSettings.showPhonetic,
                    onCheckedChange = onShowPhoneticChange
                )
                SettingsSwitch(
                    icon = Icons.Default.Image,
                    title = "卡片背景图片",
                    subtitle = "显示单词助记图片",
                    checked = userSettings.cardBackgroundEnabled,
                    onCheckedChange = onCardBackgroundChange
                )
            }

            Divider()

            // About Section
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本信息",
                    subtitle = "v1.0.0",
                    onClick = { /* TODO */ }
                )
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "开源许可",
                    subtitle = "查看开源许可信息",
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    LingDictTheme {
        SettingsContent(
            userSettings = UserSettings(
                darkMode = false,
                notificationsEnabled = true,
                dailyLearningGoal = 20,
                dailyReviewGoal = 30,
                autoPlayAudio = false,
                showPhonetic = true,
                cardBackgroundEnabled = true
            )
        )
    }
}
