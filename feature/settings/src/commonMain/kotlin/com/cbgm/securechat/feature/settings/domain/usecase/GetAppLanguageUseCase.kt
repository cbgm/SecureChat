package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.core.ui.locale.AppLanguage
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository

class GetAppLanguageUseCase(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): AppLanguage = settingsRepository.getLanguage()
}
