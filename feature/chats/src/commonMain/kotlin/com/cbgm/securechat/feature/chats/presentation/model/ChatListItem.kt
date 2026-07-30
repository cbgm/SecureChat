package com.cbgm.securechat.feature.chats.presentation.model

data class ChatListItem(
    val conversationId: String,
    val contactId: String,
    val contactName: String,
    val lastMessage: String = "",
    val timestamp: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false
)
