package com.cbgm.securechat.feature.chats.domain.model

enum class MessageDeliveryEvent {
    SEND_STARTED,
    SEND_SUCCEEDED,
    SEND_FAILED,
    RETRY_REQUESTED,
    DELIVERY_CONFIRMED,
    READ_CONFIRMED
}
