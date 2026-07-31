package com.cbgm.securechat.feature.chats.presentation.model

sealed interface GroupChatEffect {
    data object ConversationRemoved : GroupChatEffect
}
