package com.cbgm.securechat.startup

sealed interface StartupUiState {

    data object Loading : StartupUiState

    data object Ready : StartupUiState

    data class IdentityCreated(
        val isContinuing: Boolean = false
    ) : StartupUiState

    data class Error(
        val message: String
    ) : StartupUiState
}