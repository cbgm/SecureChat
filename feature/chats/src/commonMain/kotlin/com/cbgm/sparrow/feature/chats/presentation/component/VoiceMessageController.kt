package com.cbgm.sparrow.feature.chats.presentation.component

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.device.VoiceMessagePlayer
import com.cbgm.sparrow.feature.chats.device.VoiceMessageRecorder
import com.cbgm.sparrow.feature.chats.device.VoiceRecording
import com.cbgm.sparrow.feature.chats.presentation.component.model.VoiceComposerPhase
import com.cbgm.sparrow.feature.chats.presentation.component.model.VoiceComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.VoicePlaybackUiState
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

class VoiceMessageController(
    private val scope: CoroutineScope,
    private val recorder: VoiceMessageRecorder,
    private val player: VoiceMessagePlayer
) {
    private val mutableComposerState = MutableStateFlow(VoiceComposerUiState())
    val composerState: StateFlow<VoiceComposerUiState> = mutableComposerState.asStateFlow()

    private val mutablePlaybackState = MutableStateFlow(VoicePlaybackUiState())
    val playbackState: StateFlow<VoicePlaybackUiState> = mutablePlaybackState.asStateFlow()

    private var recording: VoiceRecording? = null
    private var recordingStartedAt: TimeMark? = null
    private var recordingTicker: Job? = null
    private var playbackTicker: Job? = null
    private var previewPlaying = false
    private var previewPrepared = false

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

        if (mutablePlaybackState.value.attachmentId != null) {
            mutablePlaybackState.value = VoicePlaybackUiState()
            player.stop()
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

    fun outgoingAttachment(): OutgoingMessageAttachment? =
        recording?.let { current ->
            OutgoingMessageAttachment(
                id = IdGenerator.generate(prefix = "voice"),
                type = MessageAttachmentType.VOICE,
                bytes = current.bytes,
                mimeType = current.mimeType,
                durationMilliseconds = current.durationMilliseconds
            )
        }

    fun playMessage(
        attachmentId: String,
        bytes: ByteArray,
        durationMilliseconds: Long,
        onError: (Throwable) -> Unit
    ) {
        val current = mutablePlaybackState.value
        if (current.attachmentId == attachmentId && player.isPlaying) {
            player.pause()
            playbackTicker?.cancel()
            mutablePlaybackState.value =
                current.copy(
                    positionMilliseconds = player.currentPositionMilliseconds,
                    isPlaying = false
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
        }

        val result =
            if (current.attachmentId == attachmentId && player.currentPositionMilliseconds > 0L) {
                runCatching { player.resume() }
            } else {
                player.stop()
                player.play(bytes)
            }
        result
            .onSuccess {
                mutablePlaybackState.value =
                    VoicePlaybackUiState(
                        attachmentId = attachmentId,
                        durationMilliseconds = durationMilliseconds,
                        positionMilliseconds = player.currentPositionMilliseconds,
                        isPlaying = true
                    )
                startMessageTicker(attachmentId, durationMilliseconds)
            }
            .onFailure(onError)
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
        player.stop()
        mutablePlaybackState.value = VoicePlaybackUiState()
        mutableComposerState.value =
            mutableComposerState.value.copy(
                isPlaying = false,
                playbackProgress = 0f
            )
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
            while (mutablePlaybackState.value.attachmentId == attachmentId) {
                delay(PROGRESS_INTERVAL_MILLISECONDS.milliseconds)
                val position = player.currentPositionMilliseconds
                val playing = player.isPlaying
                mutablePlaybackState.value =
                    VoicePlaybackUiState(
                        attachmentId = attachmentId,
                        durationMilliseconds = durationMilliseconds,
                        positionMilliseconds = position,
                        isPlaying = playing
                    )
                if (!playing) {
                    player.stop()
                    mutablePlaybackState.value = VoicePlaybackUiState()
                    break
                }
            }
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MILLISECONDS = 100L
    }
}
