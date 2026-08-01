package com.cbgm.securechat.notification.model

data class PendingMessageSyncResult(
    val processedEnvelopeCount: Int,
    val notifications: List<ConversationNotification>
)
