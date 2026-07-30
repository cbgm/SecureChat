package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository

class SetDeveloperEnabledUseCase(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setDeveloperModeEnabled(enabled)
    }
}
