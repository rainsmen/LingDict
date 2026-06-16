package com.lingdict.app.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lingdict.app.domain.model.Word
import com.lingdict.app.presentation.theme.LingDictTheme
import com.lingdict.app.presentation.theme.SwipeLeftRed
import com.lingdict.app.presentation.theme.SwipeRightGreen
import com.lingdict.app.presentation.theme.SwipeUpBlue
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeableCard(
    word: Word,
    onSwipeLeft: () -> Unit,   // 不认识
    onSwipeRight: () -> Unit,  // 认识
    onSwipeUp: () -> Unit,     // 收藏/标记
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false,
    onFlip: () -> Unit = {}
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val swipeThreshold = 200f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                rotationZ = offsetX.value / 20f
                alpha = 1f - (abs(offsetX.value) + abs(offsetY.value)) / 1000f
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > swipeThreshold -> {
                                    // Swipe right - 认识
                                    offsetX.animateTo(
                                        targetValue = 1000f,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                                    )
                                    onSwipeRight()
                                }
                                offsetX.value < -swipeThreshold -> {
                                    // Swipe left - 不认识
                                    offsetX.animateTo(
                                        targetValue = -1000f,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                                    )
                                    onSwipeLeft()
                                }
                                offsetY.value < -swipeThreshold -> {
                                    // Swipe up - 收藏
                                    offsetY.animateTo(
                                        targetValue = -1000f,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                                    )
                                    onSwipeUp()
                                }
                                else -> {
                                    // Reset position
                                    launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                    launch {
                                        offsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
    ) {
        FlipCard(
            word = word,
            isFlipped = isFlipped,
            onFlip = onFlip
        )

        // Swipe indicators
        if (offsetX.value > 50f) {
            SwipeIndicator(
                text = "认识",
                icon = Icons.Default.ThumbUp,
                color = SwipeRightGreen,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp)
            )
        } else if (offsetX.value < -50f) {
            SwipeIndicator(
                text = "不认识",
                icon = Icons.Default.ThumbDown,
                color = SwipeLeftRed,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp)
            )
        } else if (offsetY.value < -50f) {
            SwipeIndicator(
                text = "标记",
                icon = Icons.Default.Star,
                color = SwipeUpBlue,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
            )
        }
    }
}

@Composable
private fun SwipeIndicator(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = androidx.compose.ui.graphics.Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SwipeableCardPreview() {
    LingDictTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            SwipeableCard(
                word = Word(
                    word = "dictionary",
                    phonetic = "/ˈdɪkʃəneri/",
                    definition = "A book or electronic resource that lists the words of a language",
                    translation = "n. 字典；词典",
                    level = "CET4"
                ),
                onSwipeLeft = {},
                onSwipeRight = {},
                onSwipeUp = {}
            )
        }
    }
}
