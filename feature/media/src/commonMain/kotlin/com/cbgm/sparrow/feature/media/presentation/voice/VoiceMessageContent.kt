package com.cbgm.sparrow.feature.media.presentation.voice

import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscriptCue
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_media_voice_transcribe
import com.cbgm.sparrow.resources.feature_media_voice_transcribing
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun VoiceMessageContent(
    durationMilliseconds: Long,
    playbackPositionMilliseconds: Long,
    isPlaying: Boolean,
    waveform: List<Float>,
    transcript: String?,
    modifier: Modifier = Modifier,
    transcriptCues: List<VoiceTranscriptCue> = emptyList(),
    isTranscribing: Boolean,
    onPlayPauseClick: () -> Unit,
    onTranscribeClick: () -> Unit
) {
    val displayPlaybackPositionMilliseconds =
        rememberDisplayPlaybackPosition(
            playbackPositionMilliseconds = playbackPositionMilliseconds,
            durationMilliseconds = durationMilliseconds,
            isPlaying = isPlaying
        )
    val progress =
        if (durationMilliseconds > 0L) {
            displayPlaybackPositionMilliseconds.toFloat() / durationMilliseconds.toFloat()
        } else {
            0f
        }

    val playedWaveformColor = MaterialTheme.colorScheme.primary
    val remainingWaveformColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.Subtle)
    val transcriptionScrollState = rememberScrollState()
    val transcriptionViewportWidth = remember { mutableIntStateOf(0) }
    var transcriptionLayout by remember(transcript) { mutableStateOf<TextLayoutResult?>(null) }

    val playedCharacterPosition =
        remember(transcript, transcriptCues, displayPlaybackPositionMilliseconds, durationMilliseconds) {
            calculatePlayedCharacterPosition(
                transcript = transcript.orEmpty(),
                cues = transcriptCues,
                playbackPositionMilliseconds = displayPlaybackPositionMilliseconds,
                durationMilliseconds = durationMilliseconds
            )
        }
    val playedTextX =
        calculatePlayedTextX(
            transcript = transcript.orEmpty(),
            characterPosition = playedCharacterPosition,
            textLayout = transcriptionLayout
        )
    val transcriptionTargetScroll =
        calculateTranscriptTargetScroll(
            playbackHeadX = playedTextX,
            maxScroll = transcriptionScrollState.maxValue,
            viewportWidth = transcriptionViewportWidth.intValue
        )

    SyncTranscriptScroll(
        scrollState = transcriptionScrollState,
        isPlaying = isPlaying,
        playbackPositionMilliseconds = displayPlaybackPositionMilliseconds,
        finalCueEndMilliseconds = transcriptCues.lastOrNull()?.endMilliseconds ?: durationMilliseconds.takeIf { it > 0L },
        targetScroll = transcriptionTargetScroll
    )

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
                if (displayPlaybackPositionMilliseconds > 0L) {
                    displayPlaybackPositionMilliseconds.coerceAtMost(durationMilliseconds)
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
                            overflow = TextOverflow.Clip,
                            onTextLayout = { transcriptionLayout = it }
                        )
                        Text(
                            text = transcript,
                            modifier =
                                Modifier.drawWithContent {
                                    clipRect(right = playedTextX.coerceIn(0f, size.width)) {
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

@Composable
private fun rememberDisplayPlaybackPosition(
    playbackPositionMilliseconds: Long,
    durationMilliseconds: Long,
    isPlaying: Boolean
): Long {
    var displayedPosition by remember { mutableLongStateOf(playbackPositionMilliseconds) }
    val currentReportedPosition by rememberUpdatedState(playbackPositionMilliseconds)
    val currentDuration by rememberUpdatedState(durationMilliseconds)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    LaunchedEffect(isPlaying, playbackPositionMilliseconds) {
        if (!isPlaying) {
            displayedPosition = playbackPositionMilliseconds
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        var observedReportedPosition = currentReportedPosition
        var anchorPosition = currentReportedPosition
        var anchorFrameNanos = 0L

        while (currentIsPlaying) {
            withFrameNanos { frameNanos ->
                if (
                    anchorFrameNanos == 0L ||
                    currentReportedPosition != observedReportedPosition
                ) {
                    observedReportedPosition = currentReportedPosition
                    anchorPosition = currentReportedPosition
                    anchorFrameNanos = frameNanos
                }

                val elapsedMilliseconds = (frameNanos - anchorFrameNanos) / NANOS_PER_MILLISECOND
                displayedPosition =
                    (anchorPosition + elapsedMilliseconds)
                        .coerceIn(0L, currentDuration.coerceAtLeast(0L))
            }
        }
    }

    return displayedPosition
}

@Composable
private fun SyncTranscriptScroll(
    scrollState: ScrollState,
    isPlaying: Boolean,
    playbackPositionMilliseconds: Long,
    finalCueEndMilliseconds: Long?,
    targetScroll: Int
) {
    val currentTargetScroll by rememberUpdatedState(targetScroll)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val isAtStart = playbackPositionMilliseconds <= 0L
    val isAtEnd =
        finalCueEndMilliseconds != null &&
            playbackPositionMilliseconds >= finalCueEndMilliseconds

    LaunchedEffect(isAtStart, isAtEnd) {
        when {
            isAtStart -> scrollState.scrollTo(0)
            isAtEnd -> scrollState.scrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        while (currentIsPlaying) {
            withFrameNanos { }
            val target = currentTargetScroll.coerceIn(0, scrollState.maxValue)
            if (scrollState.value != target) {
                scrollState.scrollTo(target)
            }
        }
    }
}

private fun calculatePlayedCharacterPosition(
    transcript: String,
    cues: List<VoiceTranscriptCue>,
    playbackPositionMilliseconds: Long,
    durationMilliseconds: Long
): Float {
    if (transcript.isEmpty()) return 0f
    if (playbackPositionMilliseconds <= 0L) return 0f

    if (cues.isEmpty()) {
        if (durationMilliseconds <= 0L) return 0f
        return transcript.length *
            (playbackPositionMilliseconds.toFloat() / durationMilliseconds.toFloat()).coerceIn(0f, 1f)
    }

    var characterOffset = 0
    cues.forEach { cue ->
        val cueLength = cue.text.length
        when {
            playbackPositionMilliseconds < cue.startMilliseconds -> return characterOffset.toFloat()
            playbackPositionMilliseconds <= cue.endMilliseconds -> {
                val cueDuration = (cue.endMilliseconds - cue.startMilliseconds).coerceAtLeast(1L)
                val cueProgress =
                    ((playbackPositionMilliseconds - cue.startMilliseconds).toFloat() / cueDuration.toFloat())
                        .coerceIn(0f, 1f)
                return (characterOffset + cueLength * cueProgress).coerceAtMost(transcript.length.toFloat())
            }
        }
        characterOffset += cueLength
    }

    return transcript.length.toFloat()
}

private fun calculatePlayedTextX(
    transcript: String,
    characterPosition: Float,
    textLayout: TextLayoutResult?
): Float {
    if (transcript.isEmpty()) return 0f
    val layout = textLayout ?: return 0f
    if (characterPosition <= 0f) return 0f
    if (characterPosition >= transcript.length) return layout.size.width.toFloat()

    val characterIndex = floor(characterPosition).toInt().coerceIn(0, transcript.lastIndex)
    val characterFraction = characterPosition - characterIndex
    val bounds = layout.getBoundingBox(characterIndex)
    return bounds.left + bounds.width * characterFraction
}

private fun calculateTranscriptTargetScroll(
    playbackHeadX: Float,
    maxScroll: Int,
    viewportWidth: Int
): Int {
    if (maxScroll <= 0 || viewportWidth <= 0) return 0

    return (playbackHeadX - viewportWidth * TRANSCRIPT_FOLLOW_POSITION)
        .roundToInt()
        .coerceIn(0, maxScroll)
}

private const val TRANSCRIPT_FOLLOW_POSITION = 0.62f
private const val NANOS_PER_MILLISECOND = 1_000_000L

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
            isPlaying = true,
            waveform = emptyList(),
            transcript = "This is a preview of a longer transcribed voice message in Sparrow.",
            transcriptCues =
                listOf(
                    VoiceTranscriptCue("This is a preview ", 0L, 5_000L),
                    VoiceTranscriptCue("of a longer transcribed ", 6_000L, 16_000L),
                    VoiceTranscriptCue("voice message in Sparrow.", 17_000L, 26_000L)
                ),
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
