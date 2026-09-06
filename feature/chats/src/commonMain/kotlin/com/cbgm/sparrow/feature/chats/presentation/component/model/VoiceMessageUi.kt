package com.cbgm.sparrow.feature.chats.presentation.component.model

data class VoiceComposerUiState(
    val phase: VoiceComposerPhase = VoiceComposerPhase.READY,
    val durationMilliseconds: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val waveform: List<Float> = emptyList()
)

enum class VoiceComposerPhase {
    READY,
    RECORDING,
    RECORDED
}

data class VoicePlaybackUiState(
    val attachmentId: String? = null,
    val durationMilliseconds: Long = 0L,
    val positionMilliseconds: Long = 0L,
    val isPlaying: Boolean = false
)
