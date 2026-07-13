package com.cbgm.securechat.feature.chats.domain.model

enum class MessageContentStatus {

    /**
     * Message content is available to display.
     */
    READABLE,

    /**
     * The local encrypted-at-rest copy could not be decrypted.
     */
    LOCAL_DECRYPTION_FAILED,

    /**
     * A received plaintext packet was malformed.
     */
    INVALID_PLAINTEXT_PACKET,

    /**
     * A received encrypted transport packet could not be decrypted.
     */
    TRANSPORT_DECRYPTION_FAILED
}