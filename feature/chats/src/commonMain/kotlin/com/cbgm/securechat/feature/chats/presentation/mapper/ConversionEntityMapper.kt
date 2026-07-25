package com.cbgm.securechat.feature.chats.presentation.mapper

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.presentation.screen.ChatListItem

object ConversionEntityMapper {
    fun Conversation.toChatListItem() =
        ChatListItem(
            contactId = contactId,
            contactName = contactName,
            lastMessage = lastMessage?.text ?: "No messages yet",
            timestamp = lastMessage?.timestamp?.toString().orEmpty(),
            unreadCount = unreadCount
        )
}
