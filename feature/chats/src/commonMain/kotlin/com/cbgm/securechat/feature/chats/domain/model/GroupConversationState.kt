package com.cbgm.securechat.feature.chats.domain.model

enum class GroupConversationState {
    READY,
    INVITED,
    JOINING,
    WAITING_FOR_MEMBERS,
    DISTRIBUTING_KEYS,
    LEAVING,
    REMOVED,
    DELETED,
    DECLINED,
    EXPIRED,
    FAILED
}
