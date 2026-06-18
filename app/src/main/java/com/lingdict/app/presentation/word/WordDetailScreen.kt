package com.lingdict.app.presentation.word

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lingdict.app.domain.model.Word
import com.lingdict.app.presentation.component.FlipCard
import com.lingdict.app.presentation.theme.LingDictTheme

@Composable
fun WordDetailScreen(
    navController: NavController,
    viewModel: WordDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WordDetailContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailContent(
    uiState: WordDetailUiState,
    onEvent: (WordDetailEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("单词详情") },
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
        },
        floatingActionButton = {
            if (uiState.word != null && !uiState.isAddedToLibrary) {
                ExtendedFloatingActionButton(
                    onClick = { onEvent(WordDetailEvent.AddToLibrary) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("添加到生词库") }
                )
            }
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

                uiState.word != null -> {
                    WordDetailView(
                        word = uiState.word,
                        imageUrl = uiState.imageUrl,
                        onPlayAudio = { onEvent(WordDetailEvent.PlayAudio) }
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "😔",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateBack) {
                            Text("返回")
                        }
                    }
                }
            }

            // Success snackbar
            if (uiState.isAddedToLibrary) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("已添加到生词库")
                }
            }
        }
    }
}

@Composable
fun WordDetailView(
    word: Word,
    imageUrl: String?,
    onPlayAudio: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Flip card for main content
        FlipCard(
            word = word,
            isFlipped = isFlipped,
            onFlip = { isFlipped = !isFlipped },
            onPlayAudio = onPlayAudio,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        MeaningSection(word = word)

        Spacer(modifier = Modifier.height(16.dp))

        // Image card (if available)
        if (imageUrl != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "助记图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Examples section
        if (word.examples.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "例句",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    word.examples.forEachIndexed { index, example ->
                        if (index > 0) {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        Column {
                            Text(
                                text = example.sentenceEn,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = example.sentenceZh,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (example.source != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "— ${example.source}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "例句",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无例句",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
    }
}

@Composable
private fun MeaningSection(word: Word) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "释义",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (!word.translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (!word.definition.isNullOrBlank() && word.definition != word.translation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = word.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WordDetailScreenPreview() {
    LingDictTheme {
        WordDetailContent(
            uiState = WordDetailUiState(
                word = Word(
                    word = "dictionary",
                    phonetic = "/ˈdɪkʃəneri/",
                    definition = "A book or electronic resource that lists the words of a language (typically in alphabetical order) and gives their meaning",
                    translation = "n. 字典；词典",
                    level = "CET4",
                    examples = listOf(
                        com.lingdict.app.domain.model.Example(
                            sentenceEn = "I always keep a dictionary on my desk.",
                            sentenceZh = "我总是在桌上放一本字典。",
                            source = "Oxford"
                        ),
                        com.lingdict.app.domain.model.Example(
                            sentenceEn = "You can look up the word in the dictionary.",
                            sentenceZh = "你可以在字典里查这个单词。"
                        )
                    )
                ),
                imageUrl = null,
                isLoading = false
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
