package com.cbgm.securechat.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val parts: List<MessagePart>,
    val timestamp: Long
)