package com.cbgm.securechat.feature.chats.domain.model

data class Conversation(
    val id: String,
    val contactId: String,
    val contactName: String,
    val messages: List<ChatMessage>,
    val unreadCount: Int
) {
    val lastMessage: ChatMessage?
        get() =
            messages.maxByOrNull {
                it.timestamp
            }
}
