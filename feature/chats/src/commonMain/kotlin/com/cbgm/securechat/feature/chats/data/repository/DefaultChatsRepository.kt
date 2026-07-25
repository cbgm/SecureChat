package com.cbgm.securechat.feature.chats.data.repository

import com.cbgm.securechat.core.crypto.transport.DecodedTransportMessage
import com.cbgm.securechat.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.ProtocolPacketHandler
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class DefaultChatsRepository(
    private val chatDao: ChatDao,
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
    private val getContact: GetContact,
    private val identityExchangeStarter: IdentityExchangeStarter,
    private val protocolOutbox: ProtocolOutbox,
    private val incomingTransportMessageDecoder: IncomingTransportMessageDecoder,
    private val packetCodec: PacketCodec,
    private val protocolPacketHandler: ProtocolPacketHandler,
) : ChatsRepository {
    override fun observeConversations(): Flow<List<Conversation>> =
        chatDao.observeConversationSummaries().map { summaries ->
            summaries.map { summary ->
                summary.toDomain()
            }
        }

    override fun observeConversation(contactId: String): Flow<Conversation?> =
        chatDao.observeConversationByContactId(contactId = contactId).map { result ->
            result?.toDomain()
        }

    override suspend fun createConversation(contactId: String) {
        getOrCreateConversation(contactId = contactId)
    }

    override suspend fun sendMessage(
        contactId: String,
        text: String,
    ) {
        val normalizedText = text.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        val contact =
            getContact(contactId = contactId).getOrThrow() ?: error("Contact was not found")

        identityExchangeStarter.ensureStarted(contactId = contactId).getOrThrow()

        val conversation =
            getOrCreateConversation(contactId = contactId)

        val now = SystemClock.nowEpochMilliseconds()
        val messageId = createId(prefix = "message")

        val packet =
            ChatMessagePacket(
                packetId = createId(prefix = "packet"),
                messageId = messageId,
                sentAtEpochMilliseconds = now,
                text = normalizedText,
            )

        protocolOutbox
            .enqueue(
                contactId = contactId,
                packet = packet,
            ).getOrThrow()

        val plannedTransportMode = contact.plannedTransportMode()

        chatDao.upsertMessage(
            MessageEntity(
                id = messageId,
                conversationId = conversation.id,
                packetId = packet.packetId,
                text = normalizedText,
                transportPayload = null,
                transportMode = plannedTransportMode.name,
                contentStatus = MessageContentStatus.READABLE.name,
                deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                senderContactId = null,
                isMine = true,
                createdAtEpochMilliseconds = now,
            ),
        )

        chatDao.updateConversationTimestamp(
            conversationId = conversation.id,
            timestamp = now,
        )
    }

    override suspend fun retryMessage(
        messageId: String,
    ): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) {
                "Message ID must not be blank"
            }

            val message =
                chatDao.findMessageById(messageId = messageId)
                    ?: error("Message was not found")

            check(message.isMine) {
                "Only outgoing messages can be retried"
            }

            check(message.deliveryStatus == MessageDeliveryStatus.FAILED.name) {
                "Only failed messages can be retried"
            }

            val packetId =
                message.packetId
                    ?.takeIf(String::isNotBlank)
                    ?: error("Message has no linked protocol packet")

            val outboxItem =
                protocolOutbox
                    .findByPacketId(packetId = packetId)
                    .getOrThrow()
                    ?: error("Linked outbox item was not found")

            protocolOutbox
                .retry(itemId = outboxItem.id)
                .getOrThrow()

            val updatedRows =
                messageDeliveryStatusDao.updateDeliveryStatusByMessageId(
                    messageId = messageId,
                    deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                )

            check(updatedRows == 1) {
                "Message delivery status could not be updated"
            }
        }

    override suspend fun markConversationRead(
        contactId: String,
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val messages =
                chatDao.findMessagesAwaitingReadReceipt(
                    contactId = contactId,
                )

            messages.forEach { message ->
                val receipt =
                    ReadReceiptPacket(
                        packetId =
                            createReadReceiptPacketId(
                                messageId = message.messageId,
                            ),
                        messageId = message.messageId,
                        readAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    )

                protocolOutbox
                    .enqueue(
                        contactId = contactId,
                        packet = receipt,
                    ).getOrThrow()

                val updatedRows = chatDao.markReadReceiptSent(messageId = message.messageId)

                check(updatedRows == 1) {
                    "Incoming message could not be marked as read"
                }

                println(
                    "Read receipt queued: " +
                        "messageId=${message.messageId}, " +
                        "contactId=$contactId",
                )
            }
        }

    private fun createReadReceiptPacketId(
        messageId: String,
    ): String = "read-receipt-$messageId"

    override suspend fun receiveMessage(
        contactId: String,
        encodedTransportPayload: String,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray,
    ) {
        require(encodedTransportPayload.isNotBlank()) {
            "Incoming transport payload must not be blank"
        }

        val conversation = getOrCreateConversation(contactId = contactId)

        val receivedAt = SystemClock.nowEpochMilliseconds()

        val decodedTransport =
            incomingTransportMessageDecoder.decode(
                encodedPayload = encodedTransportPayload,
                localPublicKey = localEncryptionPublicKey,
                localPrivateKey = localEncryptionPrivateKey,
            )

        when (decodedTransport) {
            is DecodedTransportMessage.Readable -> {
                handleReadableTransportPacket(
                    contactId = contactId,
                    conversation = conversation,
                    encodedTransportPayload = encodedTransportPayload,
                    decodedTransport = decodedTransport,
                    receivedAt = receivedAt,
                )
            }

            is DecodedTransportMessage.InvalidPacket -> {
                storeFailedIncomingMessage(
                    conversation = conversation,
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Invalid transport packet",
                    transportMode = UNKNOWN_TRANSPORT_MODE,
                    contentStatus = MessageContentStatus.INVALID_PACKET,
                    receivedAt = receivedAt,
                )
            }

            is DecodedTransportMessage.InvalidPlaintext -> {
                storeFailedIncomingMessage(
                    conversation = conversation,
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Unable to read plaintext message",
                    transportMode = TransportEncryptionMode.PLAINTEXT.name,
                    contentStatus = MessageContentStatus.INVALID_PLAINTEXT_PACKET,
                    receivedAt = receivedAt,
                )
            }

            is DecodedTransportMessage.DecryptionFailed -> {
                storeFailedIncomingMessage(
                    conversation = conversation,
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Unable to decrypt secure message",
                    transportMode = TransportEncryptionMode.SEALED_BOX.name,
                    contentStatus = MessageContentStatus.TRANSPORT_DECRYPTION_FAILED,
                    receivedAt = receivedAt,
                )
            }
        }
    }

    private suspend fun handleReadableTransportPacket(
        contactId: String,
        conversation: ConversationEntity,
        encodedTransportPayload: String,
        decodedTransport: DecodedTransportMessage.Readable,
        receivedAt: Long,
    ) {
        val packet =
            packetCodec
                .decode(
                    encodedPacket = decodedTransport.plaintext,
                ).getOrElse {
                    storeFailedIncomingMessage(
                        conversation = conversation,
                        encodedTransportPayload = encodedTransportPayload,
                        text = "Invalid protocol packet",
                        transportMode = decodedTransport.mode.name,
                        contentStatus = MessageContentStatus.INVALID_PACKET,
                        receivedAt = receivedAt,
                    )

                    return
                }

        protocolPacketHandler
            .handle(
                context =
                    IncomingPacketContext(
                        contactId = contactId,
                        conversationId = conversation.id,
                        encodedTransportPayload = encodedTransportPayload,
                        transportMode = decodedTransport.mode.name,
                        receivedAtEpochMilliseconds = receivedAt,
                    ),
                packet = packet,
            ).onFailure {
                storeFailedIncomingMessage(
                    conversation = conversation,
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Unsupported or invalid protocol packet",
                    transportMode = decodedTransport.mode.name,
                    contentStatus = MessageContentStatus.INVALID_PACKET,
                    receivedAt = receivedAt,
                )
            }
    }

    private suspend fun storeFailedIncomingMessage(
        conversation: ConversationEntity,
        encodedTransportPayload: String,
        text: String,
        transportMode: String,
        contentStatus: MessageContentStatus,
        receivedAt: Long,
    ) {
        chatDao.upsertMessage(
            MessageEntity(
                id = createId(prefix = "failed-message"),
                conversationId = conversation.id,
                packetId = null,
                text = text,
                transportPayload = encodedTransportPayload,
                transportMode = transportMode,
                contentStatus = contentStatus.name,
                deliveryStatus =
                    MessageDeliveryStatus.NOT_APPLICABLE.name,
                senderContactId = conversation.contactId,
                isMine = false,
                createdAtEpochMilliseconds = receivedAt,
            ),
        )

        chatDao.updateConversationTimestamp(
            conversationId = conversation.id,
            timestamp = receivedAt,
        )
    }

    private suspend fun getOrCreateConversation(
        contactId: String,
    ): ConversationEntity {
        val existing =
            chatDao.findConversationByContactId(
                contactId = contactId,
            )

        if (existing != null) {
            return existing
        }

        val now = SystemClock.nowEpochMilliseconds()

        val conversation =
            ConversationEntity(
                id = createId(prefix = "conversation"),
                contactId = contactId,
                type = DIRECT_CONVERSATION_TYPE,
                title = null,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now,
            )

        chatDao.upsertConversation(conversation)

        return chatDao.findConversationByContactId(
            contactId = contactId,
        ) ?: error("Conversation could not be created")
    }

    private fun Contact.plannedTransportMode(): TransportEncryptionMode {
        val identity = secureChatIdentity

        val canEncrypt =
            identity != null &&
                identity.encryptionPublicKey.isNotEmpty() &&
                identity.keyExchangeStatus ==
                KeyExchangeStatus.MUTUAL

        return if (canEncrypt) {
            TransportEncryptionMode.SEALED_BOX
        } else {
            TransportEncryptionMode.PLAINTEXT
        }
    }

    private fun ConversationWithMessages.toDomain(): Conversation {
        val contactId =
            requireNotNull(conversation.contactId) {
                "Direct conversation ${conversation.id} " +
                    "has no contactId"
            }

        return Conversation(
            id = conversation.id,
            contactId = contactId,
            contactName = "",
            messages =
                messages
                    .sortedBy(MessageEntity::createdAtEpochMilliseconds)
                    .map { entity ->
                        entity.toDomain(contactId = contactId)
                    },
            unreadCount =
                messages.count { message ->
                    !message.isMine &&
                        !message.readReceiptSent &&
                        message.contentStatus == MessageContentStatus.READABLE.name
                },
        )
    }

    private fun MessageEntity.toDomain(
        contactId: String,
    ): ChatMessage =
        ChatMessage(
            id = id,
            contactId = contactId,
            text = text,
            isMine = isMine,
            timestamp = createdAtEpochMilliseconds,
            security = transportMode.toMessageSecurity(),
            contentStatus = contentStatus.toMessageContentStatus(),
            deliveryStatus =
                if (isMine) {
                    deliveryStatus.toMessageDeliveryStatus()
                } else {
                    MessageDeliveryStatus.NOT_APPLICABLE
                },
        )

    private fun ConversationSummary.toDomain(): Conversation {
        val lastMessage =
            lastMessageText?.let { text ->
                ChatMessage(
                    id = "summary-$conversationId",
                    contactId = contactId,
                    text = text,
                    isMine = true,
                    timestamp =
                        lastMessageTimestamp
                            ?: updatedAtEpochMilliseconds,
                    security = MessageSecurity.INSECURE,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
                )
            }

        return Conversation(
            id = conversationId,
            contactId = contactId,
            contactName =
                contactName
                    ?.takeIf(String::isNotBlank)
                    ?: "Unknown contact",
            messages = listOfNotNull(lastMessage),
            unreadCount = unreadCount,
        )
    }

    private fun String.toMessageSecurity(): MessageSecurity =
        if (this == TransportEncryptionMode.SEALED_BOX.name) {
            MessageSecurity.END_TO_END_ENCRYPTED
        } else {
            MessageSecurity.INSECURE
        }

    private fun String.toMessageContentStatus(): MessageContentStatus =
        MessageContentStatus.entries.firstOrNull { status ->
            status.name == this
        } ?: MessageContentStatus.INVALID_PACKET

    private fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
        MessageDeliveryStatus.entries.firstOrNull { status ->
            status.name == this
        } ?: MessageDeliveryStatus.NOT_APPLICABLE

    private fun createId(prefix: String): String {
        val timestamp = SystemClock.nowEpochMilliseconds()

        val random =
            Random.nextLong().toString().replace(
                oldValue = "-",
                newValue = "",
            )

        return "$prefix-$timestamp-$random"
    }

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
        const val UNKNOWN_TRANSPORT_MODE = "UNKNOWN"
    }
}
