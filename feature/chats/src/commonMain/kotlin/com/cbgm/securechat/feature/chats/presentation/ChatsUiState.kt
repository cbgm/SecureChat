package com.cbgm.securechat.feature.chats.presentation

data class ChatsUiState(
    val conversations: List<ChatListItem> = emptyList()
)