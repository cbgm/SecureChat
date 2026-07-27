package com.cbgm.securechat.feature.chats.data.invitation

internal fun canSendToActiveGroupMembers(activeParticipantCount: Int): Boolean = activeParticipantCount > 0
