package com.cbgm.sparrow.feature.chats.presentation.overview.model

sealed interface OverviewUiState {
    data object Loading : OverviewUiState

    data class Empty(
        val activeAutoReplyName: String? = null
    ) : OverviewUiState

    data class Content(
        val conversations: List<ConversationListItem>,
        val activeAutoReplyName: String? = null
    ) : OverviewUiState

    data class Error(
        val message: String
    ) : OverviewUiState
}
