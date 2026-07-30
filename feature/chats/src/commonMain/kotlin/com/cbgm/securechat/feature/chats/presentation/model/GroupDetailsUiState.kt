package com.cbgm.securechat.feature.chats.presentation.model

sealed interface GroupDetailsUiState {
    data object Loading : GroupDetailsUiState

    data class Content(
        val summary: GroupVerificationSummaryUiState
    ) : GroupDetailsUiState

    data class Error(
        val message: String
    ) : GroupDetailsUiState
}
