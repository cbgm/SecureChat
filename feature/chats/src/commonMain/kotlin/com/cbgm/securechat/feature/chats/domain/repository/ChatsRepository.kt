package com.cbgm.securechat.feature.chats.domain.repository

import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import kotlinx.coroutines.flow.StateFlow

interface ChatsRepository {

    val conversations: StateFlow<List<Conversation>>

    fun observeConversation(
        contactId: String
    ): StateFlow<Conversation?>

    fun createConversation(
        contactId: String,
        contactName: String
    )

    fun sendMessage(
        contactId: String,
        contactName: String,
        text: String
    )
}