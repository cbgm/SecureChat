package com.cbgm.sparrow.feature.media.presentation.voice

import com.cbgm.sparrow.feature.media.device.VoicePlayer
import com.cbgm.sparrow.feature.media.device.VoiceRecorder
import com.cbgm.sparrow.feature.media.device.VoiceRecording
import com.cbgm.sparrow.feature.media.presentation.model.VoiceComposerPhase
import com.cbgm.sparrow.feature.media.presentation.model.VoiceComposerUiState
import com.cbgm.sparrow.feature.media.presentation.model.VoiceMessageUiState
import com.cbgm.sparrow.feature.media.presentation.model.VoicePlaybackUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(kotlin.time.ExperimentalTime::class)
class VoiceController(
    private val scope: CoroutineScope,
    private val recorder: VoiceRecorder,
    private val player: VoicePlayer
) {
    private val mutableComposerState = MutableStateFlow(VoiceComposerUiState())
    val composerState: StateFlow<VoiceComposerUiState> = mutableComposerState.asStateFlow()

    private val mutableMessageState = MutableStateFlow(VoiceMessageUiState())
    val messageState: StateFlow<VoiceMessageUiState> = mutableMessageState.asStateFlow()

    private var recording: VoiceRecording? = null
    private var recordingStartedAt: TimeMark? = null
    private var recordingTicker: Job? = null
    private var playbackTicker: Job? = null
    private var previewPlaying = false
    private var previewPrepared = false
    private var preparedMessageAttachmentId: String? = null
    private var scrubbingAttachmentId: String? = null
    private var resumeAfterScrub = false

    fun startRecording(onError: (Throwable) -> Unit) {
        if (mutableComposerState.value.phase != VoiceComposerPhase.READY) return
        scope.launch {
            stopPlayback()
            recorder.start()
                .onSuccess {
                    recording = null
                    recordingStartedAt = TimeSource.Monotonic.markNow()
                    mutableComposerState.value = VoiceComposerUiState(phase = VoiceComposerPhase.RECORDING)
                    recordingTicker?.cancel()
                    recordingTicker = launch {
                        while (true) {
                            delay(PROGRESS_INTERVAL_MILLISECONDS.milliseconds)
                            mutableComposerState.value =
                                mutableComposerState.value.copy(
                                    durationMilliseconds = recordingStartedAt?.elapsedNow()?.inWholeMilliseconds ?: 0L
                                )
                        }
                    }
                }
                .onFailure(onError)
        }
    }

    fun stopRecording(onError: (Throwable) -> Unit) {
        if (mutableComposerState.value.phase != VoiceComposerPhase.RECORDING) return
        scope.launch {
            recordingTicker?.cancel()
            recorder.stop()
                .onSuccess { result ->
                    recording = result
                    recordingStartedAt = null
                    mutableComposerState.value =
                        VoiceComposerUiState(
                            phase = VoiceComposerPhase.RECORDED,
                            durationMilliseconds = result.durationMilliseconds
                        )
                }
                .onFailure(onError)
        }
    }

    fun togglePreview(onError: (Throwable) -> Unit) {
        val current = recording ?: return
        if (mutableComposerState.value.phase != VoiceComposerPhase.RECORDED) return

        if (previewPlaying) {
            player.pause()
            playbackTicker?.cancel()
            previewPlaying = false
            mutableComposerState.value = mutableComposerState.value.copy(isPlaying = false)
            return
        }

        if (mutableMessageState.value.playback.attachmentId != null) {
            updatePlayback(VoicePlaybackUiState())
            player.stop()
            preparedMessageAttachmentId = null
            clearMessageScrub()
        }

        val startResult =
            if (previewPrepared) {
                runCatching { player.resume() }
            } else {
                player.play(current.bytes)
            }
        startResult
            .onSuccess {
                previewPrepared = true
                previewPlaying = true
                mutableComposerState.value = mutableComposerState.value.copy(isPlaying = true)
                startPreviewTicker(current.durationMilliseconds)
            }
            .onFailure(onError)
    }

    fun recordedVoice(): VoiceRecording? = recording

    fun playMessage(
        attachmentId: String,
        bytes: ByteArray,
        durationMilliseconds: Long,
        onError: (Throwable) -> Unit
    ) {
        clearMessageScrub()
        val current = mutableMessageState.value.playback
        if (
            current.attachmentId == attachmentId &&
            preparedMessageAttachmentId == attachmentId &&
            player.isPlaying
        ) {
            player.pause()
            playbackTicker?.cancel()
            updatePlayback(
                current.copy(
                    positionMilliseconds = player.currentPositionMilliseconds,
                    isPlaying = false
                )
            )
            return
        }

        if (previewPrepared) {
            previewPrepared = false
            previewPlaying = false
            playbackTicker?.cancel()
            mutableComposerState.value =
                mutableComposerState.value.copy(
                    isPlaying = false,
                    playbackProgress = 0f
                )
            player.stop()
            preparedMessageAttachmentId = null
        }

        val result =
            when {
                current.attachmentId == attachmentId &&
                    preparedMessageAttachmentId == attachmentId &&
                    current.positionMilliseconds >= durationMilliseconds &&
                    durationMilliseconds > 0L ->
                    runCatching {
                        player.seekTo(0L)
                        player.resume()
                    }

                current.attachmentId == attachmentId &&
                    preparedMessageAttachmentId == attachmentId ->
                    runCatching { player.resume() }

                else -> {
                    player.stop()
                    preparedMessageAttachmentId = null
                    player.play(bytes)
                }
            }
        result
            .onSuccess {
                preparedMessageAttachmentId = attachmentId
                updatePlayback(
                    VoicePlaybackUiState(
                        attachmentId = attachmentId,
                        durationMilliseconds = durationMilliseconds,
                        positionMilliseconds = player.currentPositionMilliseconds,
                        isPlaying = true
                    )
                )
                startMessageTicker(attachmentId, durationMilliseconds)
            }
            .onFailure(onError)
    }

    fun startMessageScrub(attachmentId: String) {
        playbackTicker?.cancel()

        if (previewPrepared) {
            previewPrepared = false
            previewPlaying = false
            mutableComposerState.value =
                mutableComposerState.value.copy(
                    isPlaying = false,
                    playbackProgress = 0f
                )
            player.stop()
            preparedMessageAttachmentId = null
        }

        val current = mutableMessageState.value.playback
        scrubbingAttachmentId = attachmentId
        resumeAfterScrub =
            current.attachmentId == attachmentId &&
            preparedMessageAttachmentId == attachmentId &&
            current.isPlaying

        if (current.attachmentId == attachmentId && preparedMessageAttachmentId == attachmentId) {
            player.pause()
        } else {
            player.stop()
            preparedMessageAttachmentId = null
            updatePlayback(VoicePlaybackUiState())
        }
    }

    fun updateMessageScrubPosition(
        attachmentId: String,
        durationMilliseconds: Long,
        positionMilliseconds: Long
    ) {
        if (scrubbingAttachmentId != attachmentId) return
        updatePlayback(
            VoicePlaybackUiState(
                attachmentId = attachmentId,
                durationMilliseconds = durationMilliseconds,
                positionMilliseconds = positionMilliseconds.coerceIn(0L, durationMilliseconds.coerceAtLeast(0L)),
                isPlaying = resumeAfterScrub
            )
        )
    }

    fun finishMessageScrub(
        attachmentId: String,
        bytes: ByteArray,
        durationMilliseconds: Long,
        positionMilliseconds: Long,
        onError: (Throwable) -> Unit
    ) {
        if (scrubbingAttachmentId != attachmentId) return
        val targetPosition = positionMilliseconds.coerceIn(0L, durationMilliseconds.coerceAtLeast(0L))
        val shouldResume = resumeAfterScrub && targetPosition < durationMilliseconds

        runCatching {
            if (preparedMessageAttachmentId != attachmentId) {
                player.prepare(bytes).getOrThrow()
                preparedMessageAttachmentId = attachmentId
            }
            player.seekTo(targetPosition)
            if (shouldResume) {
                player.resume()
            }
        }.onSuccess {
            updatePlayback(
                VoicePlaybackUiState(
                    attachmentId = attachmentId,
                    durationMilliseconds = durationMilliseconds,
                    positionMilliseconds = targetPosition,
                    isPlaying = shouldResume
                )
            )
            clearMessageScrub()
            if (shouldResume) {
                startMessageTicker(attachmentId, durationMilliseconds)
            }
        }.onFailure { error ->
            player.stop()
            preparedMessageAttachmentId = null
            updatePlayback(VoicePlaybackUiState())
            clearMessageScrub()
            onError(error)
        }
    }

    fun startTranscribing(attachmentId: String): Boolean {
        if (mutableMessageState.value.transcribingAttachmentId != null) return false
        mutableMessageState.value = mutableMessageState.value.copy(transcribingAttachmentId = attachmentId)
        return true
    }

    fun stopTranscribing(attachmentId: String) {
        if (mutableMessageState.value.transcribingAttachmentId != attachmentId) return
        mutableMessageState.value = mutableMessageState.value.copy(transcribingAttachmentId = null)
    }

    fun resetComposer() {
        recordingTicker?.cancel()
        if (previewPrepared) {
            playbackTicker?.cancel()
            player.stop()
        }
        previewPlaying = false
        previewPrepared = false
        recording = null
        recordingStartedAt = null
        mutableComposerState.value = VoiceComposerUiState()
    }

    fun cancelRecording() {
        scope.launch { recorder.cancel() }
        resetComposer()
    }

    private fun stopPlayback() {
        playbackTicker?.cancel()
        previewPlaying = false
        previewPrepared = false
        preparedMessageAttachmentId = null
        clearMessageScrub()
        player.stop()
        updatePlayback(VoicePlaybackUiState())
        mutableComposerState.value =
            mutableComposerState.value.copy(
                isPlaying = false,
                playbackProgress = 0f
            )
    }

    private fun clearMessageScrub() {
        scrubbingAttachmentId = null
        resumeAfterScrub = false
    }

    private fun updatePlayback(playback: VoicePlaybackUiState) {
        mutableMessageState.value = mutableMessageState.value.copy(playback = playback)
    }

    private fun startPreviewTicker(durationMilliseconds: Long) {
        playbackTicker?.cancel()
        playbackTicker = scope.launch {
            while (previewPlaying) {
                delay(PROGRESS_INTERVAL_MILLISECONDS.milliseconds)
                val position = player.currentPositionMilliseconds
                if (!player.isPlaying) {
                    previewPlaying = false
                    previewPrepared = false
                    player.stop()
                    mutableComposerState.value =
                        mutableComposerState.value.copy(
                            isPlaying = false,
                            playbackProgress = 0f
                        )
                    break
                }
                mutableComposerState.value =
                    mutableComposerState.value.copy(
                        playbackProgress =
                            if (durationMilliseconds > 0L) {
                                position.toFloat() / durationMilliseconds.toFloat()
                            } else {
                                0f
                            }
                    )
            }
        }
    }

    private fun startMessageTicker(
        attachmentId: String,
        durationMilliseconds: Long
    ) {
        playbackTicker?.cancel()
        playbackTicker = scope.launch {
            while (mutableMessageState.value.playback.attachmentId == attachmentId) {
                delay(PROGRESS_INTERVAL_MILLISECONDS.milliseconds)
                val position = player.currentPositionMilliseconds
                val playing = player.isPlaying
                if (!playing) {
                    updatePlayback(
                        VoicePlaybackUiState(
                            attachmentId = attachmentId,
                            durationMilliseconds = durationMilliseconds,
                            positionMilliseconds = durationMilliseconds,
                            isPlaying = false
                        )
                    )
                    player.stop()
                    preparedMessageAttachmentId = null
                    delay(TRANSCRIPT_COMPLETION_HOLD_MILLISECONDS.milliseconds)

                    val current = mutableMessageState.value.playback
                    if (
                        current.attachmentId == attachmentId &&
                        !current.isPlaying &&
                        current.positionMilliseconds >= durationMilliseconds
                    ) {
                        updatePlayback(VoicePlaybackUiState())
                    }
                    break
                }

                updatePlayback(
                    VoicePlaybackUiState(
                        attachmentId = attachmentId,
                        durationMilliseconds = durationMilliseconds,
                        positionMilliseconds = position,
                        isPlaying = true
                    )
                )
            }
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MILLISECONDS = 100L
        const val TRANSCRIPT_COMPLETION_HOLD_MILLISECONDS = 300L
    }
}
