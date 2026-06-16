package com.lingdict.app.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.WordStatus
import com.lingdict.app.presentation.theme.LingDictTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordCard(
    word: String,
    translation: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    phonetic: String? = null,
    level: String? = null,
    status: WordStatus? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (phonetic != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (level != null) {
                    AssistChip(
                        onClick = { },
                        label = { Text(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = translation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (status != null) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusChip(status = status)
            }
        }
    }
}

@Composable
fun StatusChip(status: WordStatus) {
    val (text, color) = when (status) {
        WordStatus.NEW -> "新词" to MaterialTheme.colorScheme.primary
        WordStatus.LEARNING -> "学习中" to MaterialTheme.colorScheme.tertiary
        WordStatus.MASTERED -> "已掌握" to MaterialTheme.colorScheme.secondary
    }

    SuggestionChip(
        onClick = { },
        label = { Text(text) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

@Composable
fun AchievementBadge(
    name: String,
    icon: ImageVector,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Card(
        modifier = modifier.size(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                modifier = Modifier.size(48.dp),
                tint = if (isUnlocked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isUnlocked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WordCardPreview() {
    LingDictTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            WordCard(
                word = "dictionary",
                translation = "n. 字典；词典",
                phonetic = "/ˈdɪkʃəneri/",
                level = "CET4",
                status = WordStatus.LEARNING,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AchievementBadgePreview() {
    LingDictTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AchievementBadge(
                    name = "初学者",
                    icon = Icons.Default.Star,
                    isUnlocked = true,
                    description = "学习10个单词"
                )
                AchievementBadge(
                    name = "勤奋者",
                    icon = Icons.Default.Star,
                    isUnlocked = false,
                    description = "连续学习7天"
                )
            }
        }
    }
}
