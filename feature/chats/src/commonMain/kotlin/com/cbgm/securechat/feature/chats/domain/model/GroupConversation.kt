package com.cbgm.securechat.feature.chats.domain.model

data class GroupConversation(
    val id: String,
    val title: String,
    val participantContactIds: List<String>
)
