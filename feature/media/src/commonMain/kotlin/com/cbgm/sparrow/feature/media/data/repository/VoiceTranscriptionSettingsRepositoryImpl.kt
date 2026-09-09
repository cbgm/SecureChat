package com.cbgm.sparrow.feature.media.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.media.data.datasource.VoiceTranscriptionSettingsDataSource
import com.cbgm.sparrow.feature.media.domain.repository.VoiceTranscriptionSettingsRepository
import kotlinx.coroutines.flow.Flow

class VoiceTranscriptionSettingsRepositoryImpl(
    private val dataSource: VoiceTranscriptionSettingsDataSource
) : VoiceTranscriptionSettingsRepository {
    override fun observeEnabled(): Flow<Boolean> = dataSource.observeEnabled()

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> =
        safeSuspendCall {
            dataSource.setEnabled(enabled)
        }
}
