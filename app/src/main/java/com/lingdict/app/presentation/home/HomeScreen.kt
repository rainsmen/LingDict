package com.lingdict.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.lingdict.app.domain.model.WordStatus
import com.lingdict.app.presentation.component.CircularProgressWithLabel
import com.lingdict.app.presentation.component.SearchBar
import com.lingdict.app.presentation.component.WordCard
import com.lingdict.app.presentation.navigation.Screen
import com.lingdict.app.presentation.theme.LingDictTheme

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToLearn = { navController.navigate(Screen.Learn.route) }
    )

    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.onEvent(HomeEvent.ClearError)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onNavigateToLearn: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LingDict") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search bar section
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { onEvent(HomeEvent.SearchQueryChanged(it)) },
                    suggestions = uiState.searchResults.map { it.word },
                    onSuggestionClick = { onEvent(HomeEvent.WordSelected(it)) }
                )
            }

            // Search results
            if (uiState.searchResults.isNotEmpty() && uiState.searchQuery.isNotEmpty()) {
                item {
                    Text(
                        text = "搜索结果",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(uiState.searchResults) { word ->
                    WordCard(
                        word = word.word,
                        translation = word.translation,
                        phonetic = word.phonetic,
                        level = word.level,
                        onClick = { onEvent(HomeEvent.WordSelected(word.word)) }
                    )
                }
            }

            // Today's progress section
            if (uiState.searchQuery.isEmpty()) {
                item {
                    TodayProgressSection(
                        learned = uiState.todayLearned,
                        reviewed = uiState.todayReviewed
                    )
                }

                // Due words section
                if (uiState.dueWords.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "今日待复习",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = onNavigateToLearn) {
                                Text("查看全部")
                            }
                        }
                    }

                    items(uiState.dueWords.take(5)) { userWord ->
                        WordCard(
                            word = userWord.word,
                            translation = userWord.translation,
                            phonetic = userWord.phonetic,
                            status = userWord.status,
                            onClick = onNavigateToLearn
                        )
                    }
                } else {
                    item {
                        EmptyDueWordsCard(onNavigateToLearn)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TodayProgressSection(
    learned: Int,
    reviewed: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "今日学习",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularProgressWithLabel(
                    current = learned,
                    goal = 20,
                    label = "新学单词",
                    size = 100.dp
                )
                CircularProgressWithLabel(
                    current = reviewed,
                    goal = 30,
                    label = "复习单词",
                    size = 100.dp
                )
            }
        }
    }
}

@Composable
fun EmptyDueWordsCard(onNavigateToLearn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无待复习单词",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "搜索并添加单词开始学习吧",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToLearn) {
                Text("开始学习")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LingDictTheme {
        HomeContent(
            uiState = HomeUiState(
                searchQuery = "",
                dueWords = listOf(
                    UserWord(
                        id = 1,
                        word = "dictionary",
                        phonetic = "/ˈdɪkʃəneri/",
                        definition = "A book or electronic resource",
                        translation = "n. 字典；词典",
                        level = "CET4",
                        addedDate = System.currentTimeMillis(),
                        lastReviewDate = null,
                        nextReviewDate = System.currentTimeMillis(),
                        easeFactor = 2.5f,
                        interval = 1,
                        repetitions = 0,
                        status = WordStatus.LEARNING
                    )
                ),
                todayLearned = 5,
                todayReviewed = 12
            ),
            onEvent = {},
            onNavigateToLearn = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenSearchPreview() {
    LingDictTheme {
        HomeContent(
            uiState = HomeUiState(
                searchQuery = "dict",
                searchResults = listOf(
                    Word(
                        word = "dictionary",
                        phonetic = "/ˈdɪkʃəneri/",
                        definition = "A book or electronic resource",
                        translation = "n. 字典；词典",
                        level = "CET4"
                    ),
                    Word(
                        word = "dictate",
                        phonetic = "/dɪkˈteɪt/",
                        definition = "Say or read aloud",
                        translation = "v. 口述；命令",
                        level = "CET6"
                    )
                )
            ),
            onEvent = {},
            onNavigateToLearn = {}
        )
    }
}
