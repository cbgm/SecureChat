package com.cbgm.sparrow.feature.media.domain.model

data class VoiceTranscription(
    val text: String,
    val cues: List<VoiceTranscriptCue> = emptyList()
)

data class VoiceTranscriptCue(
    val text: String,
    val startMilliseconds: Long,
    val endMilliseconds: Long
)
