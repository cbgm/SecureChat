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

    /**
     * Stores one packet received from the transport layer.
     *
     * The local encryption key pair is supplied by the transport or
     * identity integration layer.
     */
    suspend fun receiveMessage(
        contactId: String,
        encodedTransportPayload: String,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    )

    suspend fun retryMessage(
        messageId: String
    ): Result<Unit>
}