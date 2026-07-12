package com.cbgm.securechat.data.database.model

data class ConversationSummary(
    val conversationId: String,
    val contactId: String,
    val contactName: String?,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long?,
    val updatedAtEpochMilliseconds: Long
)