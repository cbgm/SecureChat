package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.repository.VoiceTranscriptionSettingsRepository

class SetVoiceTranscriptionEnabledUseCase(
    private val repository: VoiceTranscriptionSettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> =
        repository.setEnabled(enabled)
}
