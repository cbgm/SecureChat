package com.cbgm.securechat.feature.chats.domain.repository

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatsRepository {

    fun observeConversations():
            Flow<List<Conversation>>

    fun observeConversation(
        contactId: String
    ): Flow<Conversation?>

    suspend fun createConversation(
        contactId: String
    )

    suspend fun sendMessage(
        contactId: String,
        text: String
    )
}