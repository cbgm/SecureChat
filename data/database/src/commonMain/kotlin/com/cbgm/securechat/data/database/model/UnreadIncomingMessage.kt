package com.cbgm.securechat.data.database.model

data class UnreadIncomingMessage(
    val messageId: String,
    val conversationId: String,
    val contactId: String
)