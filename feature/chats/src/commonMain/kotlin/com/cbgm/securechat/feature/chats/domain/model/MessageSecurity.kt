package com.cbgm.securechat.feature.chats.domain.model

enum class MessageSecurity {

    /**
     * The message has no end-to-end transport encryption.
     *
     * It can still be encrypted locally in the Room database.
     */
    INSECURE,

    /**
     * The message is encrypted using the contact's SecureChat
     * public encryption key.
     */
    END_TO_END_ENCRYPTED
}