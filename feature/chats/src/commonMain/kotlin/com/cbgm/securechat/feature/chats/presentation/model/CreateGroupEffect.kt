package com.cbgm.securechat.feature.chats.presentation.model

sealed interface CreateGroupEffect {
    data class GroupCreated(
        val conversationId: String
    ) : CreateGroupEffect
}
