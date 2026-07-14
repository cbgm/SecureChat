package com.cbgm.securechat.startup

sealed interface StartupUiState {

    data object Loading : StartupUiState

    data object Ready : StartupUiState

    data object IdentityRequired : StartupUiState

    data class Error(
        val message: String
    ) : StartupUiState
}
