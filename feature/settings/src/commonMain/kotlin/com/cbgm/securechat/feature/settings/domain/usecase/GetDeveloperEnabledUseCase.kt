package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository

class GetDeveloperEnabledUseCase(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): Boolean = settingsRepository.isDeveloperModeEnabled()
}
