package com.cbgm.securechat.feature.transport.relay.model

data class RelayTypingEvent(
    val senderId: String,
    val isTyping: Boolean
)