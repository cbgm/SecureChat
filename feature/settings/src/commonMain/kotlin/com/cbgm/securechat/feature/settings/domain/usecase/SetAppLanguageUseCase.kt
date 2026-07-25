package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.core.ui.locale.AppLanguage
import com.cbgm.securechat.core.ui.locale.setAppLanguage
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository

class SetAppLanguageUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        language: AppLanguage
    ) {
        settingsRepository.setLanguage(
            language
        )

        setAppLanguage(
            language
        )
    }
}
