package com.cbgm.securechat.feature.chats.presentation

import com.cbgm.securechat.feature.chats.domain.model.ChatMessage

data class ChatUiState(
    val contactName: String = "Contact",
    val messageText: String = "",
    val messages: List<ChatMessage> = emptyList()
)