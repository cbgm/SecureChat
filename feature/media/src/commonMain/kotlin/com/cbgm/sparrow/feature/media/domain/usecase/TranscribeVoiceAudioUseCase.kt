package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.repository.VoiceTranscriptionRepository

class TranscribeVoiceAudioUseCase(
    private val repository: VoiceTranscriptionRepository
) {
    suspend operator fun invoke(bytes: ByteArray): Result<String> = repository.transcribe(bytes)
}
