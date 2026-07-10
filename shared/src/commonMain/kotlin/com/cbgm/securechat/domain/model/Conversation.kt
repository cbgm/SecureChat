package com.cbgm.securechat.domain.model

enum class ConversationType {
    DIRECT,
    GROUP
}


data class Conversation(
    val id: String,
    val name: String?,
    val type: ConversationType
)