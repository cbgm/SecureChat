package com.cbgm.securechat.feature.settings.presentation.model

sealed interface SettingsEffect {
    data class ShowSnackbar(
        val message: String
    ) : SettingsEffect
}
