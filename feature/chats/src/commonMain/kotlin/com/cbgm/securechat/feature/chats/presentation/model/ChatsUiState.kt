package com.cbgm.securechat.feature.chats.presentation.model

import com.cbgm.securechat.feature.chats.presentation.screen.ChatListItem

data class ChatsUiState(
    val isLoading: Boolean = true,
    val conversations: List<ChatListItem> = emptyList(),
)
