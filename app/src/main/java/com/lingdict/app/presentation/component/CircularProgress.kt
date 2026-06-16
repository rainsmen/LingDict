package com.lingdict.app.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lingdict.app.presentation.theme.LingDictTheme

@Composable
fun CircularProgress(
    current: Int,
    goal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    animationDuration: Int = 1000,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val progress = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "progress"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Background circle
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$current",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/ $goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CircularProgressWithLabel(
    current: Int,
    goal: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgress(
            current = current,
            goal = goal,
            size = size
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CircularProgressPreview() {
    LingDictTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            CircularProgress(
                current = 15,
                goal = 20
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircularProgressWithLabelPreview() {
    LingDictTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularProgressWithLabel(
                    current = 15,
                    goal = 20,
                    label = "今日学习"
                )
                CircularProgressWithLabel(
                    current = 8,
                    goal = 10,
                    label = "今日复习"
                )
                CircularProgressWithLabel(
                    current = 42,
                    goal = 100,
                    label = "本周目标"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircularProgressCompletedPreview() {
    LingDictTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            CircularProgress(
                current = 20,
                goal = 20
            )
        }
    }
}
