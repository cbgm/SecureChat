package com.cbgm.securechat.feature.chats.data.repository

import com.cbgm.securechat.core.crypto.transport.DecodedTransportMessage
import com.cbgm.securechat.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.securechat.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class DefaultChatsRepository(
    private val chatDao: ChatDao,
    private val getContact: GetContact,
    private val transportMessageCipher:
    TransportMessageCipher,
    private val transportPayloadCodec:
    TransportPayloadCodec,
    private val incomingTransportMessageDecoder:
    IncomingTransportMessageDecoder
) : ChatsRepository {

    override fun observeConversations():
            Flow<List<Conversation>> {

        return chatDao
            .observeConversationSummaries()
            .map { summaries ->
                summaries.map { summary ->
                    summary.toDomain()
                }
            }
    }

    override fun observeConversation(
        contactId: String
    ): Flow<Conversation?> {

        return chatDao
            .observeConversationByContactId(
                contactId = contactId
            )
            .map { result ->
                result?.toDomain()
            }
    }

    override suspend fun createConversation(
        contactId: String
    ) {
        val existingConversation =
            chatDao.findConversationByContactId(
                contactId = contactId
            )

        if (existingConversation != null) {
            return
        }

        val now =
            SystemClock.nowEpochMilliseconds()

        chatDao.upsertConversation(
            ConversationEntity(
                id =
                    createId(
                        prefix = "conversation"
                    ),

                contactId =
                    contactId,

                createdAtEpochMilliseconds =
                    now,

                updatedAtEpochMilliseconds =
                    now
            )
        )
    }

    override suspend fun sendMessage(
        contactId: String,
        text: String
    ) {
        val normalizedText =
            text.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        val contact =
            getContact(
                contactId = contactId
            )
                .getOrThrow()
                ?: error(
                    "Contact was not found"
                )

        val conversation =
            getOrCreateConversation(
                contactId = contactId
            )

        val transportPayload =
            createOutgoingTransportPayload(
                plaintext =
                    normalizedText
                        .encodeToByteArray(),

                contact =
                    contact
            )

        val encodedTransportPayload =
            transportPayloadCodec.encode(
                payload = transportPayload
            )

        val now =
            SystemClock.nowEpochMilliseconds()

        chatDao.upsertMessage(
            MessageEntity(
                id =
                    createId(
                        prefix = "message"
                    ),

                conversationId =
                    conversation.id,

                text =
                    normalizedText,

                transportPayload =
                    encodedTransportPayload,

                transportMode =
                    transportPayload
                        .mode
                        .name,

                contentStatus =
                    MessageContentStatus
                        .READABLE
                        .name,

                isMine =
                    true,

                createdAtEpochMilliseconds =
                    now
            )
        )

        chatDao.updateConversationTimestamp(
            conversationId =
                conversation.id,

            timestamp =
                now
        )
    }

    override suspend fun receiveMessage(
        contactId: String,
        encodedTransportPayload: String,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    ) {
        require(
            encodedTransportPayload.isNotBlank()
        ) {
            "Incoming transport payload must not be blank"
        }

        val conversation =
            getOrCreateConversation(
                contactId = contactId
            )

        val decoded =
            incomingTransportMessageDecoder.decode(
                encodedPayload =
                    encodedTransportPayload,

                localPublicKey =
                    localEncryptionPublicKey,

                localPrivateKey =
                    localEncryptionPrivateKey
            )

        val storedMessage =
            decoded.toStoredIncomingMessage()

        val now =
            SystemClock.nowEpochMilliseconds()

        chatDao.upsertMessage(
            MessageEntity(
                id =
                    createId(
                        prefix = "message"
                    ),

                conversationId =
                    conversation.id,

                text =
                    storedMessage.text,

                transportPayload =
                    encodedTransportPayload,

                transportMode =
                    storedMessage.transportMode,

                contentStatus =
                    storedMessage.contentStatus.name,

                isMine =
                    false,

                createdAtEpochMilliseconds =
                    now
            )
        )

        chatDao.updateConversationTimestamp(
            conversationId =
                conversation.id,

            timestamp =
                now
        )
    }

    /**
     * Transport encryption is active only in state 3:
     *
     * 1. no remote public keys -> plaintext
     * 2. one-way key exchange -> plaintext
     * 3. mutual key exchange -> sealed box
     */
    private suspend fun createOutgoingTransportPayload(
        plaintext: ByteArray,
        contact: Contact
    ): EncryptedTransportPayload {

        val identity =
            contact.secureChatIdentity

        if (
            identity == null ||
            identity
                .encryptionPublicKey
                .isEmpty()
        ) {
            return createPlaintextPayload(
                plaintext = plaintext
            )
        }

        if (
            identity.keyExchangeStatus !=
            KeyExchangeStatus.MUTUAL
        ) {
            return createPlaintextPayload(
                plaintext = plaintext
            )
        }

        return transportMessageCipher
            .encryptForRecipient(
                plaintext = plaintext,

                recipientPublicKey =
                    identity
                        .encryptionPublicKey
            )
            .getOrThrow()
    }

    private fun createPlaintextPayload(
        plaintext: ByteArray
    ): EncryptedTransportPayload {

        return EncryptedTransportPayload(
            version =
                TRANSPORT_VERSION,

            mode =
                TransportEncryptionMode
                    .PLAINTEXT,

            payload =
                plaintext
        )
    }

    private suspend fun getOrCreateConversation(
        contactId: String
    ): ConversationEntity {

        val existing =
            chatDao.findConversationByContactId(
                contactId = contactId
            )

        if (existing != null) {
            return existing
        }

        val now =
            SystemClock.nowEpochMilliseconds()

        val created =
            ConversationEntity(
                id =
                    createId(
                        prefix = "conversation"
                    ),

                contactId =
                    contactId,

                createdAtEpochMilliseconds =
                    now,

                updatedAtEpochMilliseconds =
                    now
            )

        chatDao.upsertConversation(
            conversation = created
        )

        return chatDao
            .findConversationByContactId(
                contactId = contactId
            )
            ?: error(
                "Conversation could not be created"
            )
    }

    private fun DecodedTransportMessage
            .toStoredIncomingMessage():
            StoredIncomingMessage {

        return when (this) {
            is DecodedTransportMessage.Readable -> {
                StoredIncomingMessage(
                    text =
                        plaintext.decodeToString(),

                    transportMode =
                        mode.name,

                    contentStatus =
                        MessageContentStatus
                            .READABLE
                )
            }

            is DecodedTransportMessage.InvalidPacket -> {
                StoredIncomingMessage(
                    text =
                        "Invalid message packet",

                    transportMode =
                        UNKNOWN_TRANSPORT_MODE,

                    contentStatus =
                        MessageContentStatus
                            .INVALID_PACKET
                )
            }

            is DecodedTransportMessage.InvalidPlaintext -> {
                StoredIncomingMessage(
                    text =
                        "Unable to read plaintext message",

                    transportMode =
                        TransportEncryptionMode
                            .PLAINTEXT
                            .name,

                    contentStatus =
                        MessageContentStatus
                            .INVALID_PLAINTEXT_PACKET
                )
            }

            is DecodedTransportMessage.DecryptionFailed -> {
                StoredIncomingMessage(
                    text =
                        "Unable to decrypt secure message",

                    transportMode =
                        TransportEncryptionMode
                            .SEALED_BOX
                            .name,

                    contentStatus =
                        MessageContentStatus
                            .TRANSPORT_DECRYPTION_FAILED
                )
            }
        }
    }

    private fun ConversationWithMessages.toDomain():
            Conversation {

        return Conversation(
            id =
                conversation.id,

            contactId =
                conversation.contactId,

            contactName =
                "",

            messages =
                messages
                    .sortedBy {
                        it.createdAtEpochMilliseconds
                    }
                    .map { entity ->
                        entity.toDomain(
                            contactId =
                                conversation.contactId
                        )
                    }
        )
    }

    private fun MessageEntity.toDomain(
        contactId: String
    ): ChatMessage {

        return ChatMessage(
            id =
                id,

            contactId =
                contactId,

            text =
                text,

            isMine =
                isMine,

            timestamp =
                createdAtEpochMilliseconds,

            security =
                transportMode
                    .toMessageSecurity(),

            contentStatus =
                contentStatus
                    .toMessageContentStatus()
        )
    }

    private fun ConversationSummary.toDomain():
            Conversation {

        val lastMessage =
            lastMessageText?.let { text ->
                ChatMessage(
                    id =
                        "summary-$conversationId",

                    contactId =
                        contactId,

                    text =
                        text,

                    isMine =
                        true,

                    timestamp =
                        lastMessageTimestamp
                            ?: updatedAtEpochMilliseconds,

                    security =
                        MessageSecurity.INSECURE,

                    contentStatus =
                        MessageContentStatus
                            .READABLE
                )
            }

        return Conversation(
            id =
                conversationId,

            contactId =
                contactId,

            contactName =
                contactName
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Unknown contact",

            messages =
                listOfNotNull(
                    lastMessage
                )
        )
    }

    private fun String.toMessageSecurity():
            MessageSecurity {

        return if (
            this ==
            TransportEncryptionMode
                .SEALED_BOX
                .name
        ) {
            MessageSecurity
                .END_TO_END_ENCRYPTED
        } else {
            MessageSecurity
                .INSECURE
        }
    }

    private fun String.toMessageContentStatus():
            MessageContentStatus {

        return when (this) {
            MessageContentStatus
                .READABLE
                .name -> {
                MessageContentStatus.READABLE
            }

            MessageContentStatus
                .INVALID_PACKET
                .name -> {
                MessageContentStatus.INVALID_PACKET
            }

            MessageContentStatus
                .INVALID_PLAINTEXT_PACKET
                .name -> {
                MessageContentStatus
                    .INVALID_PLAINTEXT_PACKET
            }

            MessageContentStatus
                .TRANSPORT_DECRYPTION_FAILED
                .name -> {
                MessageContentStatus
                    .TRANSPORT_DECRYPTION_FAILED
            }

            else -> {
                MessageContentStatus
                    .INVALID_PACKET
            }
        }
    }

    private fun createId(
        prefix: String
    ): String {

        val timestamp =
            SystemClock.nowEpochMilliseconds()

        val random =
            Random.nextLong()
                .toString()
                .replace(
                    oldValue = "-",
                    newValue = ""
                )

        return "$prefix-$timestamp-$random"
    }

    private data class StoredIncomingMessage(
        val text: String,
        val transportMode: String,
        val contentStatus:
        MessageContentStatus
    )

    private companion object {
        const val TRANSPORT_VERSION =
            1

        const val UNKNOWN_TRANSPORT_MODE =
            "UNKNOWN"
    }
}