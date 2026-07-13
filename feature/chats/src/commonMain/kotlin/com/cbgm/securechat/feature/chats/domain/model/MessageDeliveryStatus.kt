package com.cbgm.securechat.feature.chats.domain.model

enum class MessageDeliveryStatus {

    /**
     * Used for incoming messages.
     */
    NOT_APPLICABLE,

    /**
     * Packet is persisted in the outbox.
     */
    QUEUED,

    /**
     * The outbox processor is currently attempting delivery.
     */
    SENDING,

    /**
     * The configured transport accepted the packet.
     */
    SENT,

    /**
     * The transport attempt failed.
     */
    FAILED
}