package com.lingdict.app.presentation.learn

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.presentation.component.SwipeableCard
import com.lingdict.app.presentation.theme.LingDictTheme

@Composable
fun LearnScreen(
    navController: NavController,
    viewModel: LearnViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LearnContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnContent(
    uiState: LearnUiState,
    onEvent: (LearnEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.totalWords > 0) {
                            "学习进度 ${uiState.currentIndex + 1}/${uiState.totalWords}"
                        } else {
                            "学习"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.currentWord != null -> {
                    LearnCardSection(
                        uiState = uiState,
                        onEvent = onEvent
                    )
                }

                uiState.totalWords == 0 -> {
                    EmptyLearnState(
                        modifier = Modifier.align(Alignment.Center),
                        onNavigateBack = onNavigateBack
                    )
                }

                else -> {
                    CompletedState(
                        modifier = Modifier.align(Alignment.Center),
                        onNavigateBack = onNavigateBack
                    )
                }
            }

            // Progress indicator at top
            if (uiState.totalWords > 0) {
                LinearProgressIndicator(
                    progress = { (uiState.currentIndex + 1).toFloat() / uiState.totalWords },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
fun LearnCardSection(
    uiState: LearnUiState,
    onEvent: (LearnEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "学习提示",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 点击卡片翻转查看释义\n• 向右滑动表示认识\n• 向左滑动表示不认识\n• 向上滑动表示很熟悉",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Swipeable card
        uiState.currentWord?.let { userWord ->
            SwipeableCard(
                word = Word(
                    word = userWord.word.word,
                    phonetic = userWord.word.phonetic,
                    definition = userWord.word.definition,
                    translation = userWord.word.translation,
                    level = userWord.word.level
                ),
                isFlipped = uiState.isFlipped,
                onFlip = { onEvent(LearnEvent.FlipCard) },
                onSwipeLeft = { onEvent(LearnEvent.SwipeLeft()) },
                onSwipeRight = { onEvent(LearnEvent.SwipeRight()) },
                onSwipeUp = { onEvent(LearnEvent.SwipeUp()) },
                onPlayAudio = { onEvent(LearnEvent.PlayAudio) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Remaining words indicator
        if (uiState.remainingWords.isNotEmpty()) {
            Text(
                text = "还有 ${uiState.remainingWords.size} 个单词待学习",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun EmptyLearnState(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无待学习单词",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "去首页添加单词开始学习吧",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateBack) {
            Text("返回首页")
        }
    }
}

@Composable
fun CompletedState(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✅",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "今日学习完成！",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "恭喜你完成了所有待复习单词",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateBack) {
            Text("返回首页")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LearnScreenPreview() {
    LingDictTheme {
        LearnContent(
            uiState = LearnUiState(
                currentWord = UserWord(
                    id = 1,
                    word = Word(
                        word = "dictionary",
                        phonetic = "/ˈdɪkʃəneri/",
                        definition = "A book or electronic resource that lists the words of a language",
                        translation = "n. 字典；词典",
                        level = "CET4"
                    ),
                    addedDate = System.currentTimeMillis(),
                    lastReviewDate = null,
                    nextReviewDate = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 1,
                    repetitions = 0,
                    status = WordStatus.LEARNING
                ),
                totalWords = 10,
                currentIndex = 3,
                isFlipped = false
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyLearnStatePreview() {
    LingDictTheme {
        Surface {
            EmptyLearnState(onNavigateBack = {})
        }
    }
}
