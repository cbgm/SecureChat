package com.cbgm.securechat.feature.chats.domain.model

data class ChatMessage(
    val id: String,
    val contactId: String,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val senderContactId: String? = null,
    val senderName: String? = null,
    val deliveryProgress: MessageDeliveryProgress = MessageDeliveryProgress()
)
