package com.cbgm.sparrow.feature.media.device

interface VoiceRecorder {
    suspend fun start(): Result<Unit>

    suspend fun stop(): Result<VoiceRecording>

    suspend fun cancel()
}
