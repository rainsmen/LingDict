package com.lingdict.app.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.presentation.component.AchievementBadge
import com.lingdict.app.presentation.theme.LingDictTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsContent(
    uiState: StatisticsUiState,
    onEvent: (StatisticsEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习统计") },
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
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Summary cards
            item {
                SummaryCards(
                    totalWords = uiState.totalWordsLearned,
                    masteredWords = uiState.masteredWords,
                    learningStreak = uiState.learningStreak
                )
            }

            // Period selector
            item {
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { onEvent(StatisticsEvent.SelectPeriod(it)) }
                )
            }

            // Learning trend chart
            if (uiState.dailyRecords.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "学习趋势",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LearningTrendChart(
                                records = uiState.dailyRecords.takeLast(30),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }
            }

            // Word distribution pie chart
            if (uiState.wordDistribution.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "单词状态分布",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            WordDistributionChart(
                                distribution = uiState.wordDistribution,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }
                    }
                }
            }

            // Learning calendar heatmap
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "学习日历",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LearningCalendarHeatmap(
                            records = uiState.dailyRecords,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Achievements
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "成就徽章",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AchievementsGrid(
                            totalWords = uiState.totalWordsLearned,
                            streak = uiState.learningStreak
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SummaryCards(
    totalWords: Int,
    masteredWords: Int,
    learningStreak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            icon = Icons.Default.MenuBook,
            title = "总学习",
            value = "$totalWords",
            subtitle = "个单词",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            icon = Icons.Default.CheckCircle,
            title = "已掌握",
            value = "$masteredWords",
            subtitle = "个单词",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            icon = Icons.Default.LocalFireDepartment,
            title = "连续天数",
            value = "$learningStreak",
            subtitle = "天",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimePeriod.values().forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        when (period) {
                            TimePeriod.WEEK -> "周"
                            TimePeriod.MONTH -> "月"
                            TimePeriod.YEAR -> "年"
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LearningTrendChart(
    records: List<DailyRecord>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        if (records.isEmpty()) return@Canvas

        val maxValue = records.maxOfOrNull { it.wordsLearned + it.wordsReviewed }?.toFloat() ?: 1f
        val barWidth = size.width / records.size
        val heightScale = size.height / maxValue

        records.forEachIndexed { index, record ->
            val totalWords = (record.wordsLearned + record.wordsReviewed).toFloat()
            val barHeight = totalWords * heightScale

            // Draw bar
            drawRect(
                color = primaryColor,
                topLeft = Offset(index * barWidth + barWidth * 0.1f, size.height - barHeight),
                size = Size(barWidth * 0.8f, barHeight)
            )
        }

        // Draw baseline
        drawLine(
            color = Color.Gray,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f
        )
    }
}

@Composable
fun WordDistributionChart(
    distribution: Map<WordStatus, Int>,
    modifier: Modifier = Modifier
) {
    val total = distribution.values.sum().toFloat()
    if (total == 0f) return

    val colors = mapOf(
        WordStatus.NEW to MaterialTheme.colorScheme.primary,
        WordStatus.LEARNING to MaterialTheme.colorScheme.tertiary,
        WordStatus.MASTERED to MaterialTheme.colorScheme.secondary
    )

    val labels = mapOf(
        WordStatus.NEW to "新词",
        WordStatus.LEARNING to "学习中",
        WordStatus.MASTERED to "已掌握"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie chart
        Canvas(modifier = Modifier.size(150.dp)) {
            var startAngle = -90f

            distribution.forEach { (status, count) ->
                val sweepAngle = (count / total) * 360f
                val color = colors[status] ?: Color.Gray

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )

                startAngle += sweepAngle
            }

            // Draw center circle for donut effect
            drawCircle(
                color = Color.White,
                radius = size.minDimension / 4
            )
        }

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            distribution.forEach { (status, count) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(colors[status] ?: Color.Gray)
                    )
                    Column {
                        Text(
                            text = labels[status] ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$count 个 (${(count / total * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LearningCalendarHeatmap(
    records: List<DailyRecord>,
    modifier: Modifier = Modifier
) {
    val maxValue = records.maxOfOrNull { it.wordsLearned }?.toFloat() ?: 1f
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(records.take(49)) { record ->
            val intensity = if (maxValue > 0) record.wordsLearned / maxValue else 0f

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(
                        color = primaryColor.copy(alpha = intensity.coerceIn(0.1f, 1f)),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "少",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    primaryColor.copy(alpha = 0.2f),
                    MaterialTheme.shapes.small
                )
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    primaryColor.copy(alpha = 0.5f),
                    MaterialTheme.shapes.small
                )
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    primaryColor.copy(alpha = 0.8f),
                    MaterialTheme.shapes.small
                )
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    primaryColor,
                    MaterialTheme.shapes.small
                )
        )
        Text(
            text = "多",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AchievementsGrid(
    totalWords: Int,
    streak: Int
) {
    val achievements = listOf(
        Achievement("初学者", Icons.Default.Star, totalWords >= 10, "学习10个单词"),
        Achievement("勤奋者", Icons.Default.LocalFireDepartment, streak >= 7, "连续学习7天"),
        Achievement("探索者", Icons.Default.Explore, totalWords >= 50, "学习50个单词"),
        Achievement("学霸", Icons.Default.School, totalWords >= 100, "学习100个单词"),
        Achievement("大师", Icons.Default.EmojiEvents, totalWords >= 500, "学习500个单词"),
        Achievement("坚持者", Icons.Default.FavoriteBorder, streak >= 30, "连续学习30天")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(250.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(achievements) { achievement ->
            AchievementBadge(
                name = achievement.name,
                icon = achievement.icon,
                isUnlocked = achievement.isUnlocked,
                description = achievement.description
            )
        }
    }
}

data class Achievement(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isUnlocked: Boolean,
    val description: String
)

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    LingDictTheme {
        StatisticsContent(
            uiState = StatisticsUiState(
                totalWordsLearned = 125,
                masteredWords = 45,
                learningStreak = 12,
                wordDistribution = mapOf(
                    WordStatus.NEW to 30,
                    WordStatus.LEARNING to 50,
                    WordStatus.MASTERED to 45
                ),
                dailyRecords = List(30) { index ->
                    DailyRecord(
                        date = LocalDate.now().minusDays(index.toLong()),
                        wordsLearned = (5..15).random(),
                        wordsReviewed = (10..25).random(),
                        testsCompleted = (0..3).random(),
                        accuracy = 0.85f
                    )
                },
                selectedPeriod = TimePeriod.MONTH
            ),
            onEvent = {}
        )
    }
}
