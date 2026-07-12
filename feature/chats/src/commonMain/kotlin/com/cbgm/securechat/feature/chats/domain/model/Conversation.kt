package com.cbgm.securechat.feature.chats.domain.model

data class Conversation(
    val contactId: String,
    val contactName: String,
    val messages: List<ChatMessage>
) {
    val lastMessage: ChatMessage?
        get() = messages.lastOrNull()
}