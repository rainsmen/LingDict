package com.lingdict.app.presentation.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.lingdict.app.domain.model.Question
import com.lingdict.app.presentation.theme.LingDictTheme

@Composable
fun TestScreen(
    navController: NavController,
    viewModel: TestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TestContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestContent(
    uiState: TestUiState,
    onEvent: (TestEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            uiState.testType == null -> "测试"
                            uiState.isTestCompleted -> "测试完成"
                            else -> "题目 ${uiState.currentQuestionIndex + 1}/${uiState.totalQuestions}"
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

                uiState.isTestCompleted -> {
                    TestResultScreen(
                        score = uiState.score,
                        correctCount = uiState.correctCount,
                        totalQuestions = uiState.totalQuestions,
                        onRestart = { onEvent(TestEvent.RestartTest) },
                        onNavigateBack = onNavigateBack
                    )
                }

                uiState.testType == null -> {
                    TestTypeSelection(onEvent = onEvent)
                }

                uiState.currentQuestion != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Progress indicator
                        LinearProgressIndicator(
                            progress = { (uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Question content
                        QuestionContent(
                            question = uiState.currentQuestion,
                            selectedAnswer = uiState.selectedAnswer,
                            isAnswerSubmitted = uiState.isAnswerSubmitted,
                            isCorrect = uiState.isCorrect,
                            onSelectAnswer = { onEvent(TestEvent.SelectAnswer(it)) },
                            onPlayAudio = { onEvent(TestEvent.PlayAudio) }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Action buttons
                        if (uiState.isAnswerSubmitted) {
                            Button(
                                onClick = { onEvent(TestEvent.NextQuestion) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (uiState.currentQuestionIndex < uiState.totalQuestions - 1) {
                                        "下一题"
                                    } else {
                                        "查看结果"
                                    }
                                )
                            }
                        } else {
                            Button(
                                onClick = { onEvent(TestEvent.SubmitAnswer) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.selectedAnswer != null
                            ) {
                                Text("提交答案")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestTypeSelection(onEvent: (TestEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "选择测试类型",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TestTypeCard(
            icon = Icons.Default.CheckCircle,
            title = "选择题",
            description = "从四个选项中选择正确答案",
            onClick = { onEvent(TestEvent.SelectTestType(TestType.MULTIPLE_CHOICE)) }
        )

        TestTypeCard(
            icon = Icons.Default.Edit,
            title = "填空题",
            description = "根据提示填写完整单词",
            onClick = { onEvent(TestEvent.SelectTestType(TestType.FILL_IN_BLANK)) }
        )

        TestTypeCard(
            icon = Icons.Default.VolumeUp,
            title = "听力题",
            description = "听音频选择正确单词",
            onClick = { onEvent(TestEvent.SelectTestType(TestType.LISTENING)) }
        )

        TestTypeCard(
            icon = Icons.Default.Done,
            title = "判断题",
            description = "判断释义是否正确",
            onClick = { onEvent(TestEvent.SelectTestType(TestType.TRUE_FALSE)) }
        )
    }
}

@Composable
fun TestTypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
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
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuestionContent(
    question: Question,
    selectedAnswer: String?,
    isAnswerSubmitted: Boolean,
    isCorrect: Boolean?,
    onSelectAnswer: (String) -> Unit,
    onPlayAudio: () -> Unit
) {
    when (question) {
        is Question.MultipleChoice -> {
            MultipleChoiceQuestion(
                question = question,
                selectedAnswer = selectedAnswer,
                isAnswerSubmitted = isAnswerSubmitted,
                isCorrect = isCorrect,
                onSelectAnswer = onSelectAnswer
            )
        }

        is Question.FillInBlank -> {
            FillInBlankQuestion(
                question = question,
                answer = selectedAnswer ?: "",
                isAnswerSubmitted = isAnswerSubmitted,
                isCorrect = isCorrect,
                onAnswerChange = onSelectAnswer
            )
        }

        is Question.Listening -> {
            ListeningQuestion(
                question = question,
                selectedAnswer = selectedAnswer,
                isAnswerSubmitted = isAnswerSubmitted,
                isCorrect = isCorrect,
                onSelectAnswer = onSelectAnswer,
                onPlayAudio = onPlayAudio
            )
        }

        is Question.TrueFalse -> {
            TrueFalseQuestion(
                question = question,
                selectedAnswer = selectedAnswer?.toBoolean(),
                isAnswerSubmitted = isAnswerSubmitted,
                isCorrect = isCorrect,
                onSelectAnswer = { onSelectAnswer(it.toString()) }
            )
        }
    }
}

@Composable
fun MultipleChoiceQuestion(
    question: Question.MultipleChoice,
    selectedAnswer: String?,
    isAnswerSubmitted: Boolean,
    isCorrect: Boolean?,
    onSelectAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Word to translate
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.word,
                    style = MaterialTheme.typography.displaySmall
                )
                if (question.phonetic != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.phonetic,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Text(
            text = "选择正确的释义：",
            style = MaterialTheme.typography.titleMedium
        )

        // Options
        question.options.forEach { option ->
            val isSelected = selectedAnswer == option
            val isCorrectAnswer = isAnswerSubmitted && option == question.correctAnswer
            val isWrongAnswer = isAnswerSubmitted && isSelected && !isCorrectAnswer

            val containerColor = when {
                isCorrectAnswer -> MaterialTheme.colorScheme.primaryContainer
                isWrongAnswer -> MaterialTheme.colorScheme.errorContainer
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                onClick = { if (!isAnswerSubmitted) onSelectAnswer(option) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = if (isSelected && !isAnswerSubmitted) {
                    CardDefaults.outlinedCardBorder()
                } else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCorrectAnswer) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "正确",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else if (isWrongAnswer) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "错误",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Feedback
        if (isAnswerSubmitted && isCorrect != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isCorrect) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCorrect) "回答正确！" else "回答错误",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun FillInBlankQuestion(
    question: Question.FillInBlank,
    answer: String,
    isAnswerSubmitted: Boolean,
    isCorrect: Boolean?,
    onAnswerChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = "填写完整单词：",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = question.displayWord,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnswerSubmitted,
            placeholder = { Text("输入缺失部分") },
            singleLine = true
        )

        if (isAnswerSubmitted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect == true) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCorrect == true) "回答正确！" else "回答错误",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (isCorrect == false) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "正确答案：${question.fullWord}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListeningQuestion(
    question: Question.Listening,
    selectedAnswer: String?,
    isAnswerSubmitted: Boolean,
    isCorrect: Boolean?,
    onSelectAnswer: (String) -> Unit,
    onPlayAudio: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "播放",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(
                    text = "点击播放单词发音",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            text = "听音频，选择正确的单词：",
            style = MaterialTheme.typography.titleMedium
        )

        question.options.forEach { option ->
            val isSelected = selectedAnswer == option
            val isCorrectAnswer = isAnswerSubmitted && option == question.correctAnswer
            val isWrongAnswer = isAnswerSubmitted && isSelected && !isCorrectAnswer

            Card(
                onClick = { if (!isAnswerSubmitted) onSelectAnswer(option) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCorrectAnswer -> MaterialTheme.colorScheme.primaryContainer
                        isWrongAnswer -> MaterialTheme.colorScheme.errorContainer
                        isSelected -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCorrectAnswer) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "正确",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else if (isWrongAnswer) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "错误",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (isAnswerSubmitted && isCorrect != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Text(
                    text = if (isCorrect) "回答正确！" else "回答错误",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TrueFalseQuestion(
    question: Question.TrueFalse,
    selectedAnswer: Boolean?,
    isAnswerSubmitted: Boolean,
    isCorrect: Boolean?,
    onSelectAnswer: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.word,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = question.statement,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = "判断以上释义是否正确：",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // True button
            val isTrueSelected = selectedAnswer == true
            val isTrueCorrect = isAnswerSubmitted && question.correctAnswer == true
            val isTrueWrong = isAnswerSubmitted && isTrueSelected && !isTrueCorrect

            Card(
                onClick = { if (!isAnswerSubmitted) onSelectAnswer(true) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isTrueCorrect -> MaterialTheme.colorScheme.primaryContainer
                        isTrueWrong -> MaterialTheme.colorScheme.errorContainer
                        isTrueSelected -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "正确",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "正确",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // False button
            val isFalseSelected = selectedAnswer == false
            val isFalseCorrect = isAnswerSubmitted && question.correctAnswer == false
            val isFalseWrong = isAnswerSubmitted && isFalseSelected && !isFalseCorrect

            Card(
                onClick = { if (!isAnswerSubmitted) onSelectAnswer(false) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isFalseCorrect -> MaterialTheme.colorScheme.primaryContainer
                        isFalseWrong -> MaterialTheme.colorScheme.errorContainer
                        isFalseSelected -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "错误",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "错误",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        if (isAnswerSubmitted && isCorrect != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Text(
                    text = if (isCorrect) "回答正确！" else "回答错误",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TestResultScreen(
    score: Int,
    correctCount: Int,
    totalQuestions: Int,
    onRestart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                score >= 90 -> "🎉"
                score >= 70 -> "😊"
                score >= 60 -> "😐"
                else -> "😔"
            },
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "测试完成！",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score 分",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "正确 $correctCount / $totalQuestions 题",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重新测试")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回首页")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TestTypeSelectionPreview() {
    LingDictTheme {
        TestTypeSelection(onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
fun TestResultPreview() {
    LingDictTheme {
        TestResultScreen(
            score = 85,
            correctCount = 17,
            totalQuestions = 20,
            onRestart = {},
            onNavigateBack = {}
        )
    }
}
