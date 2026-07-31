package com.cbgm.securechat.feature.settings.presentation.model

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.ui.locale.AppLanguage

sealed interface SettingsEvent {
    data object LanguagePickerOpened : SettingsEvent

    data object LanguagePickerDismissed : SettingsEvent

    data class LanguageSelected(
        val language: AppLanguage
    ) : SettingsEvent

    data class DirectIdentitySetupModeChanged(
        val mode: DirectIdentitySetupMode
    ) : SettingsEvent

    data class BlockUnknownContactInvitesChanged(
        val enabled: Boolean
    ) : SettingsEvent

    data object VersionRowTapped : SettingsEvent
}
