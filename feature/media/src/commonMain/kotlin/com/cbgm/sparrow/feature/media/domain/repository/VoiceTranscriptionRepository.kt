package com.cbgm.sparrow.feature.media.domain.repository

import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscription

interface VoiceTranscriptionRepository {
    suspend fun transcribe(bytes: ByteArray): Result<VoiceTranscription>
}
