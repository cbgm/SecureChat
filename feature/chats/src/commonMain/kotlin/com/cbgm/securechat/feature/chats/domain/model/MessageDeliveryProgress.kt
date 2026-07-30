package com.cbgm.securechat.feature.chats.domain.model

data class MessageDeliveryProgress(
    val recipientCount: Int = 0,
    val deliveredCount: Int = 0,
    val readCount: Int = 0
)
