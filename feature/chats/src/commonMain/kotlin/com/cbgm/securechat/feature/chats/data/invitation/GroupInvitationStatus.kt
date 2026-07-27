package com.cbgm.securechat.feature.chats.data.invitation

enum class GroupInvitationStatus {
    INVITE_SENT,
    WAITING_FOR_IDENTITY,
    IDENTITY_READY,
    AWAITING_ACCEPTANCE,
    JOIN_SENT,
    WELCOME_SENT,
    DECLINED,
    EXPIRED,
    FAILED,
    ACTIVE
}
