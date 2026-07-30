package com.cbgm.securechat.feature.chats.domain.model

enum class GroupConversationState {
    READY,
    INVITED,
    JOINING,
    WAITING_FOR_MEMBERS,
    DISTRIBUTING_KEYS,
    DECLINED,
    EXPIRED,
    FAILED
}
