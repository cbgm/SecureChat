package com.cbgm.sparrow.feature.media.presentation.voice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_media_voice_transcribe
import com.cbgm.sparrow.resources.feature_media_voice_transcribing
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun VoiceMessageContent(
    durationMilliseconds: Long,
    playbackPositionMilliseconds: Long,
    isPlaying: Boolean,
    waveform: List<Float>,
    transcript: String?,
    isTranscribing: Boolean,
    onPlayPauseClick: () -> Unit,
    onTranscribeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress =
        if (durationMilliseconds > 0L) {
            playbackPositionMilliseconds.toFloat() / durationMilliseconds.toFloat()
        } else {
            0f
        }

    val playedWaveformColor = MaterialTheme.colorScheme.primary
    val remainingWaveformColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.Subtle)
    val transcriptionProgress = progress.coerceIn(0f, 1f)
    val transcriptionScrollState = rememberScrollState()
    val transcriptionViewportWidth = remember { mutableIntStateOf(0) }

    LaunchedEffect(
        isPlaying,
        playbackPositionMilliseconds,
        durationMilliseconds,
        transcriptionScrollState.maxValue,
        transcriptionViewportWidth.intValue
    ) {
        when {
            !isPlaying && playbackPositionMilliseconds <= 0L -> transcriptionScrollState.scrollTo(0)
            durationMilliseconds > 0L && playbackPositionMilliseconds >= durationMilliseconds ->
                transcriptionScrollState.scrollTo(transcriptionScrollState.maxValue)
            isPlaying && durationMilliseconds > 0L -> {
                val viewportWidth = transcriptionViewportWidth.intValue
                val contentWidth = transcriptionScrollState.maxValue + viewportWidth
                val playbackHeadX = contentWidth * transcriptionProgress
                val targetScroll =
                    (playbackHeadX - viewportWidth / 2f)
                        .roundToInt()
                        .coerceIn(0, transcriptionScrollState.maxValue)
                transcriptionScrollState.scrollTo(targetScroll)
            }
        }
    }

    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(Dimens.MessageInput.sendButtonWidth)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = playedWaveformColor
                )
            }

            VoiceWaveform(
                waveform = waveform,
                progress = progress,
                playedColor = playedWaveformColor,
                remainingColor = remainingWaveformColor,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(Dimens.MessageInput.buttonHeight)
                        .padding(horizontal = MaterialTheme.spacing.small)
            )

            val displayedDurationMilliseconds =
                if (playbackPositionMilliseconds > 0L) {
                    playbackPositionMilliseconds.coerceAtMost(durationMilliseconds)
                } else {
                    durationMilliseconds
                }

            Text(
                text = formatVoiceDuration(displayedDurationMilliseconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            isTranscribing ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.MessageBubble.progressSize),
                        strokeWidth = Dimens.MessageBubble.progressStrokeWidth
                    )
                    Text(
                        text = stringResource(Res.string.feature_media_voice_transcribing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            !transcript.isNullOrBlank() ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { transcriptionViewportWidth.intValue = it.width }
                ) {
                    Box(modifier = Modifier.horizontalScroll(transcriptionScrollState)) {
                        Text(
                            text = transcript,
                            style = MaterialTheme.typography.bodyMedium,
                            color = remainingWaveformColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = transcript,
                            modifier =
                                Modifier.drawWithContent {
                                    clipRect(right = size.width * transcriptionProgress) {
                                        this@drawWithContent.drawContent()
                                    }
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = playedWaveformColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }

            else ->
                Text(
                    text = stringResource(Res.string.feature_media_voice_transcribe),
                    modifier = Modifier.clickable(onClick = onTranscribeClick),
                    style = MaterialTheme.typography.labelMedium,
                    color = playedWaveformColor
                )
        }
    }
}

@Preview
@Composable
private fun VoiceMessageContentPreview() {
    SparrowTheme {
        VoiceMessageContent(
            durationMilliseconds = 18_000L,
            playbackPositionMilliseconds = 6_000L,
            isPlaying = true,
            waveform = emptyList(),
            transcript = null,
            isTranscribing = false,
            onPlayPauseClick = {},
            onTranscribeClick = {}
        )
    }
}

@Preview
@Composable
private fun VoiceMessageContentTranscriptPreview() {
    SparrowTheme {
        VoiceMessageContent(
            durationMilliseconds = 42_000L,
            playbackPositionMilliseconds = 17_000L,
            isPlaying = false,
            waveform = emptyList(),
            transcript = "This is a preview of a longer transcribed voice message in Sparrow.",
            isTranscribing = false,
            onPlayPauseClick = {},
            onTranscribeClick = {}
        )
    }
}

@Preview
@Composable
private fun VoiceMessageContentTranscribingPreview() {
    SparrowTheme {
        VoiceMessageContent(
            durationMilliseconds = 24_000L,
            playbackPositionMilliseconds = 0L,
            isPlaying = false,
            waveform = emptyList(),
            transcript = null,
            isTranscribing = true,
            onPlayPauseClick = {},
            onTranscribeClick = {}
        )
    }
}
