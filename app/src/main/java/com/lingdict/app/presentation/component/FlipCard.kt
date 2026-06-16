package com.lingdict.app.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lingdict.app.domain.model.Word
import com.lingdict.app.presentation.theme.LingDictTheme

@Composable
fun FlipCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onPlayAudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "cardFlip"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = if (rotation > 90f) 180f else 0f
                }
        ) {
            if (rotation <= 90f) {
                // Front side - Word only
                CardFront(
                    word = word,
                    onPlayAudio = onPlayAudio
                )
            } else {
                // Back side - Definition and translation
                CardBack(word = word)
            }
        }
    }
}

@Composable
private fun CardFront(
    word: Word,
    onPlayAudio: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )

            if (!word.phonetic.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onPlayAudio) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Play pronunciation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (word.tag != null) {
                Spacer(modifier = Modifier.height(16.dp))
                SuggestionChip(
                    onClick = { },
                    label = { Text(word.tag) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "点击查看释义",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CardBack(word: Word) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // English Definition
            if (!word.definition.isNullOrEmpty()) {
                Text(
                    text = "Definition",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = word.definition,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Chinese Translation
            Text(
                text = "释义",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = word.translation.orEmpty(),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Hint at bottom
        Text(
            text = "点击返回",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FlipCardFrontPreview() {
    LingDictTheme {
        var isFlipped by remember { mutableStateOf(false) }

        Surface(modifier = Modifier.padding(16.dp)) {
            FlipCard(
                word = Word(
                    word = "dictionary",
                    phonetic = "/ˈdɪkʃəneri/",
                    definition = "A book or electronic resource that lists the words of a language",
                    translation = "n. 字典；词典",
                    level = "CET4"
                ),
                isFlipped = isFlipped,
                onFlip = { isFlipped = !isFlipped }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FlipCardBackPreview() {
    LingDictTheme {
        var isFlipped by remember { mutableStateOf(true) }

        Surface(modifier = Modifier.padding(16.dp)) {
            FlipCard(
                word = Word(
                    word = "dictionary",
                    phonetic = "/ˈdɪkʃəneri/",
                    definition = "A book or electronic resource that lists the words of a language",
                    translation = "n. 字典；词典",
                    level = "CET4"
                ),
                isFlipped = isFlipped,
                onFlip = { isFlipped = !isFlipped }
            )
        }
    }
}
