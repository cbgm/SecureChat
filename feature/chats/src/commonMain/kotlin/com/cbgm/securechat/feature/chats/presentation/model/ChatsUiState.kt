package com.cbgm.securechat.feature.chats.presentation.model

sealed interface ChatsUiState {
    data object Loading : ChatsUiState

    data object Empty : ChatsUiState

    data class Content(
        val conversations: List<ChatListItem>
    ) : ChatsUiState

    data class Error(
        val message: String
    ) : ChatsUiState
}
