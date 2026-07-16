package com.cbgm.securechat.feature.chats.presentation.model

import com.cbgm.securechat.feature.chats.presentation.screen.ChatListItem

data class ChatsUiState(
    val conversations: List<ChatListItem> = emptyList()
)