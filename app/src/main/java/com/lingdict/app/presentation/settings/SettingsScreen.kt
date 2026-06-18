package com.lingdict.app.presentation.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.SharePdf -> {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        effect.file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "分享生词库 PDF")
                    )
                }
            }
        }
    }

    SettingsContent(
        userSettings = userSettings,
        isExportingVocabulary = uiState.isExportingVocabulary,
        exportError = uiState.exportError,
        isTestingApis = uiState.isTestingApis,
        apiTestResult = uiState.apiTestResult,
        onExportVocabulary = viewModel::exportVocabularyPdf,
        onDismissExportError = viewModel::clearExportError,
        onDarkModeChange = viewModel::updateDarkMode,
        onNotificationsChange = viewModel::updateNotifications,
        onDailyLearningGoalChange = viewModel::updateDailyLearningGoal,
        onDailyReviewGoalChange = viewModel::updateDailyReviewGoal,
        onAutoPlayAudioChange = viewModel::updateAutoPlayAudio,
        onShowPhoneticChange = viewModel::updateShowPhonetic,
        onCardBackgroundChange = viewModel::updateCardBackground,
        onYoudaoEnabledChange = viewModel::updateYoudaoEnabled,
        onYoudaoCredentialsChange = viewModel::updateYoudaoCredentials,
        onPexelsApiKeyChange = viewModel::updatePexelsApiKey,
        onTestApiSettings = viewModel::testApiSettings,
        onClearApiTestResult = viewModel::clearApiTestResult,
        onOnlineLookupPreferredChange = viewModel::updateOnlineLookupPreferred,
        onFreeDictionaryEnabledChange = viewModel::updateFreeDictionaryEnabled,
        onDatamuseEnabledChange = viewModel::updateDatamuseEnabled,
        onMerriamSettingsChange = viewModel::updateMerriamSettings,
        onWordsApiSettingsChange = viewModel::updateWordsApiSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    userSettings: UserSettings,
    isExportingVocabulary: Boolean = false,
    exportError: String? = null,
    isTestingApis: Boolean = false,
    apiTestResult: String? = null,
    onExportVocabulary: () -> Unit = {},
    onDismissExportError: () -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit = {},
    onNotificationsChange: (Boolean) -> Unit = {},
    onDailyLearningGoalChange: (Int) -> Unit = {},
    onDailyReviewGoalChange: (Int) -> Unit = {},
    onAutoPlayAudioChange: (Boolean) -> Unit = {},
    onShowPhoneticChange: (Boolean) -> Unit = {},
    onCardBackgroundChange: (Boolean) -> Unit = {},
    onYoudaoEnabledChange: (Boolean) -> Unit = {},
    onYoudaoCredentialsChange: (String, String) -> Unit = { _, _ -> },
    onPexelsApiKeyChange: (String) -> Unit = {},
    onTestApiSettings: (ApiSettingsTestRequest) -> Unit = {},
    onClearApiTestResult: () -> Unit = {},
    onOnlineLookupPreferredChange: (Boolean) -> Unit = {},
    onFreeDictionaryEnabledChange: (Boolean) -> Unit = {},
    onDatamuseEnabledChange: (Boolean) -> Unit = {},
    onMerriamSettingsChange: (Boolean, String) -> Unit = { _, _ -> },
    onWordsApiSettingsChange: (Boolean, String, String) -> Unit = { _, _, _ -> }
) {
    var learningGoalDialogVisible by remember { mutableStateOf(false) }
    var reviewGoalDialogVisible by remember { mutableStateOf(false) }
    var learningGoalText by remember(userSettings.dailyLearningGoal) {
        mutableStateOf(userSettings.dailyLearningGoal.toString())
    }
    var reviewGoalText by remember(userSettings.dailyReviewGoal) {
        mutableStateOf(userSettings.dailyReviewGoal.toString())
    }
    var apiDialogVisible by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
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
                    onClick = { learningGoalDialogVisible = true }
                )
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "每日复习目标",
                    subtitle = "${userSettings.dailyReviewGoal}个复习单词",
                    onClick = { reviewGoalDialogVisible = true }
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

            // Online API Section
            SettingsSection(title = "在线词典") {
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = "在线查询与 API",
                    subtitle = if (userSettings.onlineLookupPreferred) {
                        "在线优先；有道、Free Dictionary、Merriam、WordsAPI、Datamuse"
                    } else {
                        "本地优先；有道、Free Dictionary、Merriam、WordsAPI、Datamuse"
                    },
                    onClick = { apiDialogVisible = true }
                )
            }

            Divider()

            // Data Section
            SettingsSection(title = "数据") {
                SettingsItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "导出生词库",
                    subtitle = if (isExportingVocabulary) "正在生成 PDF..." else "生成并分享 PDF 文件",
                    enabled = !isExportingVocabulary,
                    onClick = onExportVocabulary
                )
            }

            Divider()

            // About Section
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本信息",
                    subtitle = "v1.0.0",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "开源许可",
                    subtitle = "MIT License",
                    onClick = { }
                )
            }
        }
    }

    if (apiDialogVisible) {
        ApiSettingsDialog(
            userSettings = userSettings,
            onDismiss = { apiDialogVisible = false },
            onYoudaoEnabledChange = onYoudaoEnabledChange,
            onYoudaoCredentialsChange = onYoudaoCredentialsChange,
            isTestingApis = isTestingApis,
            apiTestResult = apiTestResult,
            onPexelsApiKeyChange = onPexelsApiKeyChange,
            onTestApiSettings = onTestApiSettings,
            onClearApiTestResult = onClearApiTestResult,
            onOnlineLookupPreferredChange = onOnlineLookupPreferredChange,
            onFreeDictionaryEnabledChange = onFreeDictionaryEnabledChange,
            onDatamuseEnabledChange = onDatamuseEnabledChange,
            onMerriamSettingsChange = onMerriamSettingsChange,
            onWordsApiSettingsChange = onWordsApiSettingsChange
        )
    }

    if (learningGoalDialogVisible) {
        GoalDialog(
            title = "每日学习目标",
            value = learningGoalText,
            onValueChange = { learningGoalText = it },
            onDismiss = { learningGoalDialogVisible = false },
            onConfirm = {
                learningGoalText.toIntOrNull()?.takeIf { it > 0 }?.let(onDailyLearningGoalChange)
                learningGoalDialogVisible = false
            }
        )
    }

    if (exportError != null) {
        AlertDialog(
            onDismissRequest = onDismissExportError,
            title = { Text("导出失败") },
            text = { Text(exportError) },
            confirmButton = {
                TextButton(onClick = onDismissExportError) { Text("确定") }
            }
        )
    }

    if (reviewGoalDialogVisible) {
        GoalDialog(
            title = "每日复习目标",
            value = reviewGoalText,
            onValueChange = { reviewGoalText = it },
            onDismiss = { reviewGoalDialogVisible = false },
            onConfirm = {
                reviewGoalText.toIntOrNull()?.takeIf { it > 0 }?.let(onDailyReviewGoalChange)
                reviewGoalDialogVisible = false
            }
        )
    }
}

@Composable
private fun ApiSettingsDialog(
    userSettings: UserSettings,
    isTestingApis: Boolean,
    apiTestResult: String?,
    onDismiss: () -> Unit,
    onYoudaoEnabledChange: (Boolean) -> Unit,
    onYoudaoCredentialsChange: (String, String) -> Unit,
    onPexelsApiKeyChange: (String) -> Unit,
    onTestApiSettings: (ApiSettingsTestRequest) -> Unit,
    onClearApiTestResult: () -> Unit,
    onOnlineLookupPreferredChange: (Boolean) -> Unit,
    onFreeDictionaryEnabledChange: (Boolean) -> Unit,
    onDatamuseEnabledChange: (Boolean) -> Unit,
    onMerriamSettingsChange: (Boolean, String) -> Unit,
    onWordsApiSettingsChange: (Boolean, String, String) -> Unit
) {
    var youdaoAppKey by remember(userSettings.youdaoAppKey) { mutableStateOf(userSettings.youdaoAppKey) }
    var youdaoAppSecret by remember(userSettings.youdaoAppSecret) { mutableStateOf(userSettings.youdaoAppSecret) }
    var pexelsApiKey by remember(userSettings.pexelsApiKey) { mutableStateOf(userSettings.pexelsApiKey) }
    var merriamApiKey by remember(userSettings.merriamApiKey) { mutableStateOf(userSettings.merriamApiKey) }
    var wordsApiKey by remember(userSettings.wordsApiKey) { mutableStateOf(userSettings.wordsApiKey) }
    var wordsApiHost by remember(userSettings.wordsApiHost) { mutableStateOf(userSettings.wordsApiHost) }
    var onlineLookupPreferred by remember(userSettings.onlineLookupPreferred) { mutableStateOf(userSettings.onlineLookupPreferred) }
    var youdaoEnabled by remember(userSettings.youdaoEnabled) { mutableStateOf(userSettings.youdaoEnabled) }
    var freeDictionaryEnabled by remember(userSettings.freeDictionaryEnabled) { mutableStateOf(userSettings.freeDictionaryEnabled) }
    var datamuseEnabled by remember(userSettings.datamuseEnabled) { mutableStateOf(userSettings.datamuseEnabled) }
    var merriamEnabled by remember(userSettings.merriamEnabled) { mutableStateOf(userSettings.merriamEnabled) }
    var wordsApiEnabled by remember(userSettings.wordsApiEnabled) { mutableStateOf(userSettings.wordsApiEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("在线查询与 API") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("在线优先")
                        Text(
                            text = "开启后先查在线词典，失败再回退本地词库",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = onlineLookupPreferred, onCheckedChange = { onlineLookupPreferred = it })
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("有道词典", modifier = Modifier.weight(1f))
                    Switch(checked = youdaoEnabled, onCheckedChange = { youdaoEnabled = it })
                }
                OutlinedTextField(
                    value = youdaoAppKey,
                    onValueChange = { youdaoAppKey = it },
                    label = { Text("有道 APP_KEY") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = youdaoAppSecret,
                    onValueChange = { youdaoAppSecret = it },
                    label = { Text("有道 APP_SECRET") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Free Dictionary", modifier = Modifier.weight(1f))
                    Switch(checked = freeDictionaryEnabled, onCheckedChange = { freeDictionaryEnabled = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Datamuse", modifier = Modifier.weight(1f))
                    Switch(checked = datamuseEnabled, onCheckedChange = { datamuseEnabled = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Merriam-Webster Learner", modifier = Modifier.weight(1f))
                    Switch(checked = merriamEnabled, onCheckedChange = { merriamEnabled = it })
                }
                OutlinedTextField(
                    value = merriamApiKey,
                    onValueChange = { merriamApiKey = it },
                    label = { Text("Merriam-Webster API_KEY") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("WordsAPI / RapidAPI", modifier = Modifier.weight(1f))
                    Switch(checked = wordsApiEnabled, onCheckedChange = { wordsApiEnabled = it })
                }
                OutlinedTextField(
                    value = wordsApiKey,
                    onValueChange = { wordsApiKey = it },
                    label = { Text("WordsAPI RapidAPI Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wordsApiHost,
                    onValueChange = { wordsApiHost = it },
                    label = { Text("WordsAPI RapidAPI Host") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Divider()
                OutlinedTextField(
                    value = pexelsApiKey,
                    onValueChange = { pexelsApiKey = it },
                    label = { Text("Pexels API_KEY") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        onTestApiSettings(
                            ApiSettingsTestRequest(
                                youdaoEnabled = youdaoEnabled,
                                youdaoAppKey = youdaoAppKey,
                                youdaoAppSecret = youdaoAppSecret,
                                freeDictionaryEnabled = freeDictionaryEnabled,
                                datamuseEnabled = datamuseEnabled,
                                merriamEnabled = merriamEnabled,
                                merriamApiKey = merriamApiKey,
                                wordsApiEnabled = wordsApiEnabled,
                                wordsApiKey = wordsApiKey,
                                wordsApiHost = wordsApiHost,
                                pexelsApiKey = pexelsApiKey
                            )
                        )
                    },
                    enabled = !isTestingApis,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isTestingApis) "检测中..." else "检测 API 设置")
                }
                apiTestResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onYoudaoEnabledChange(youdaoEnabled)
                onYoudaoCredentialsChange(youdaoAppKey, youdaoAppSecret)
                onPexelsApiKeyChange(pexelsApiKey)
                onOnlineLookupPreferredChange(onlineLookupPreferred)
                onFreeDictionaryEnabledChange(freeDictionaryEnabled)
                onDatamuseEnabledChange(datamuseEnabled)
                onMerriamSettingsChange(merriamEnabled, merriamApiKey)
                onWordsApiSettingsChange(wordsApiEnabled, wordsApiKey, wordsApiHost)
                onClearApiTestResult()
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = {
                onClearApiTestResult()
                onDismiss()
            }) { Text("取消") }
        }
    )
}

@Composable
private fun GoalDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(3)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
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
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
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
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
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
                cardBackgroundEnabled = true,
                youdaoEnabled = true,
                onlineLookupPreferred = false,
                freeDictionaryEnabled = true,
                datamuseEnabled = true,
                merriamEnabled = false,
                wordsApiEnabled = false
            )
        )
    }
}
