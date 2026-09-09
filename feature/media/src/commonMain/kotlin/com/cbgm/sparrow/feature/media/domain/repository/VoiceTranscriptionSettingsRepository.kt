package com.cbgm.sparrow.feature.media.domain.repository

import kotlinx.coroutines.flow.Flow

interface VoiceTranscriptionSettingsRepository {
    fun observeEnabled(): Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean): Result<Unit>
}
