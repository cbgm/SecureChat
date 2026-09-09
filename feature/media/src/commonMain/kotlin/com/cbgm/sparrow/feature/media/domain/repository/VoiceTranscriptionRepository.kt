package com.cbgm.sparrow.feature.media.domain.repository

interface VoiceTranscriptionRepository {
    suspend fun transcribe(bytes: ByteArray): Result<String>
}
