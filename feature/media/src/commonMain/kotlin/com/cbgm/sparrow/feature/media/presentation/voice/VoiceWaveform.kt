package com.cbgm.sparrow.feature.media.presentation.voice

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import kotlin.math.max
import kotlin.math.sin

private val PlaceholderWaveform =
    listOf(
        0.24f,
        0.42f,
        0.68f,
        0.36f,
        0.82f,
        0.54f,
        0.32f,
        0.74f,
        0.46f,
        0.9f,
        0.58f,
        0.3f,
        0.64f,
        0.4f,
        0.78f,
        0.5f,
        0.28f,
        0.7f,
        0.44f,
        0.86f,
        0.52f,
        0.34f,
        0.66f,
        0.38f
    )

@Composable
fun VoiceWaveform(
    waveform: List<Float>,
    progress: Float,
    playedColor: Color,
    remainingColor: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    val transition = rememberInfiniteTransition()
    val animationPhase =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(
                        durationMillis = RECORDING_ANIMATION_DURATION_MILLISECONDS,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
        ).value

    Canvas(modifier = modifier) {
        val values = waveform.ifEmpty { PlaceholderWaveform }
        if (values.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas

        val slotWidth = size.width / values.size
        val barWidth = max(1f, slotWidth * 0.42f)
        val radius = barWidth / 2f
        val progressIndex = progress.coerceIn(0f, 1f) * values.size

        values.forEachIndexed { index, amplitude ->
            val animatedAmplitude =
                if (animated) {
                    val wave =
                        ((sin((animationPhase * TWO_PI) + index * BAR_PHASE_OFFSET) + 1f) / 2f)
                    (amplitude * RECORDING_BASE_AMPLITUDE + wave * RECORDING_ANIMATION_AMPLITUDE)
                        .coerceIn(MINIMUM_AMPLITUDE, 1f)
                } else {
                    amplitude.coerceIn(MINIMUM_AMPLITUDE, 1f)
                }
            val barHeight = size.height * animatedAmplitude
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            val top = (size.height - barHeight) / 2f

            drawRoundRect(
                color = if (index < progressIndex) playedColor else remainingColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

fun formatVoiceDuration(durationMilliseconds: Long): String {
    val totalSeconds = (durationMilliseconds.coerceAtLeast(0L) / 1_000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private const val RECORDING_ANIMATION_DURATION_MILLISECONDS = 900
private const val TWO_PI = 6.2831855f
private const val BAR_PHASE_OFFSET = 0.72f
private const val RECORDING_BASE_AMPLITUDE = 0.45f
private const val RECORDING_ANIMATION_AMPLITUDE = 0.55f
private const val MINIMUM_AMPLITUDE = 0.12f

@Preview
@Composable
private fun VoiceWaveformPreview() {
    SparrowTheme {
        VoiceWaveform(
            waveform = PlaceholderWaveform,
            progress = 0.45f,
            playedColor = MaterialTheme.colorScheme.primary,
            remainingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.Subtle),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        )
    }
}
