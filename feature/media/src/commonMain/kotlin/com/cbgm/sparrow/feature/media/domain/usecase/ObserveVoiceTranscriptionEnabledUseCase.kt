package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.repository.VoiceTranscriptionSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveVoiceTranscriptionEnabledUseCase(
    private val repository: VoiceTranscriptionSettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observeEnabled()
}
