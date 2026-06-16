package com.lingdict.app.presentation.splash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingdict.app.data.local.importer.ImportProgress

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkAndImport(context)
    }

    LaunchedEffect(state) {
        if (state is SplashState.Completed) {
            onNavigateToMain()
        }
    }

    SplashContent(
        state = state,
        onRetry = { viewModel.retry(context) }
    )
}

@Composable
fun SplashContent(
    state: SplashState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is SplashState.Checking -> {
                CheckingView()
            }

            is SplashState.Importing -> {
                ImportingView(progress = state.progress)
            }

            is SplashState.Completed -> {
                // 将自动导航到主界面
                CircularProgressIndicator()
            }

            is SplashState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
fun CheckingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "正在检查词库...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ImportingView(progress: ImportProgress) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Logo或图标
        Text(
            text = "📚",
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = "LingDict",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 进度文本
        Text(
            text = "正在初始化词库...",
            style = MaterialTheme.typography.titleMedium
        )

        // 进度条
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${progress.current.coerceAtMost(progress.total)} / ${progress.total} (${progress.percentage}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "首次启动需要导入词库\n请稍候，约30-60秒",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "😔",
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = "初始化失败",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
