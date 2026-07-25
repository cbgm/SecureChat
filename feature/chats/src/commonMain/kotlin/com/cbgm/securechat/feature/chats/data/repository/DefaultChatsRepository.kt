package com.cbgm.securechat.feature.chats.data.repository

import com.cbgm.securechat.core.crypto.transport.DecodedTransportMessage
import com.cbgm.securechat.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.ProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.GroupConversation
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryProgress
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class DefaultChatsRepository(
    private val chatDao: ChatDao,
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val getContact: GetContact,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val identityExchangeStarter: IdentityExchangeStarter,
    private val protocolOutbox: ProtocolOutbox,
    private val incomingTransportMessageDecoder: IncomingTransportMessageDecoder,
    private val packetCodec: PacketCodec,
    private val protocolPacketHandler: ProtocolPacketHandler
) : ChatsRepository {
    override fun observeConversations(): Flow<List<Conversation>> =
        chatDao.observeConversationSummaries().map { summaries ->
            summaries.map { summary ->
                summary.toDomain()
            }
        }

    override fun observeConversation(conversationId: String): Flow<Conversation?> =
        combine(
            chatDao.observeConversationWithMessagesById(conversationId),
            chatDao.observeConversationParticipants(conversationId),
            messageRecipientStateDao.observeByConversationId(conversationId)
        ) { result, participants, recipientStates ->
            result?.toDomain(
                participantContactIds = participants.map { it.contactId },
                recipientStates = recipientStates
            )
        }

    override suspend fun getOrCreateDirectConversation(contactId: String): String = getOrCreateConversation(contactId).id

    override suspend fun createGroupConversation(
        title: String,
        contactIds: Set<String>
    ): String {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Group title must not be blank" }
        require(contactIds.size >= MIN_GROUP_PARTICIPANT_COUNT) { "A group requires at least one contact" }

        val contacts =
            contactIds.map { contactId ->
                getContact(contactId).getOrThrow() ?: error("Contact was not found: $contactId")
            }
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val now = SystemClock.nowEpochMilliseconds()
        val conversationId = createId(prefix = "group")
        val conversation =
            ConversationEntity(
                id = conversationId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = normalizedTitle,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        val participants =
            contacts.map { contact ->
                ConversationParticipantEntity(
                    conversationId = conversationId,
                    contactId = contact.id,
                    role = GROUP_MEMBER_ROLE,
                    joinedAtEpochMilliseconds = now
                )
            }
        val memberPayloads =
            buildList {
                add(
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = localIdentity.encryptionPublicKey,
                        signingPublicKey = localIdentity.signingPublicKey,
                        role = GROUP_OWNER_ROLE,
                        phoneNumber = localPhoneNumber
                    )
                )
                contacts.forEach { contact ->
                    val phoneNumber =
                        contact.preferredPhoneNumber?.value
                            ?: contact.phoneNumbers.firstOrNull()?.value
                            ?: error("Contact has no phone number: ${contact.id}")
                    val identity = contact.secureChatIdentity

                    add(
                        GroupMemberPayload(
                            displayName = contact.displayName,
                            encryptionPublicKey = identity?.encryptionPublicKey ?: byteArrayOf(),
                            signingPublicKey = identity?.signingPublicKey ?: byteArrayOf(),
                            role = GROUP_MEMBER_ROLE,
                            phoneNumber = phoneNumber
                        )
                    )
                }
            }

        chatDao.createGroupConversation(conversation, participants)

        contacts.forEach { contact ->
            identityExchangeStarter.ensureStarted(contact.id).getOrThrow()
            protocolOutbox
                .enqueue(
                    contactId = contact.id,
                    packet =
                        GroupCreatedPacket(
                            packetId = createId(prefix = "group-created-packet"),
                            groupId = conversationId,
                            title = normalizedTitle,
                            createdAtEpochMilliseconds = now,
                            members = memberPayloads
                        )
                ).getOrThrow()
        }

        return conversationId
    }

    override fun observeGroupConversation(conversationId: String): Flow<GroupConversation?> =
        combine(
            chatDao.observeConversationById(conversationId),
            chatDao.observeConversationParticipants(conversationId)
        ) { conversation, participants ->
            if (conversation == null || conversation.type != GROUP_CONVERSATION_TYPE) {
                null
            } else {
                GroupConversation(
                    id = conversation.id,
                    title = conversation.title.orEmpty(),
                    participantContactIds = participants.map { it.contactId }
                )
            }
        }

    override suspend fun sendGroupMessage(
        conversationId: String,
        text: String
    ): Result<Unit> =
        runCatching {
            val normalizedText = text.trim()
            require(normalizedText.isNotEmpty()) { "Message text must not be blank" }

            val conversation =
                chatDao.findConversationById(conversationId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }

            val participants = chatDao.findConversationParticipants(conversationId)
            check(participants.isNotEmpty()) { "Group has no participants" }

            val now = SystemClock.nowEpochMilliseconds()
            val messageId = createId(prefix = "group-message")
            val packets =
                participants.associateWith { participant ->
                    GroupChatMessagePacket(
                        packetId = createId(prefix = "group-message-packet"),
                        groupId = conversationId,
                        messageId = messageId,
                        sentAtEpochMilliseconds = now,
                        text = normalizedText
                    )
                }
            val recipientStates =
                packets.map { (participant, packet) ->
                    MessageRecipientStateEntity(
                        messageId = messageId,
                        contactId = participant.contactId,
                        packetId = packet.packetId,
                        deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                        lastError = null,
                        updatedAtEpochMilliseconds = now
                    )
                }

            chatDao.upsertOutgoingGroupMessage(
                message =
                    MessageEntity(
                        id = messageId,
                        conversationId = conversationId,
                        packetId = null,
                        text = normalizedText,
                        transportPayload = null,
                        transportMode = TransportEncryptionMode.SEALED_BOX.name,
                        contentStatus = MessageContentStatus.READABLE.name,
                        deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                        senderContactId = null,
                        isMine = true,
                        createdAtEpochMilliseconds = now
                    ),
                recipientStates = recipientStates,
                timestamp = now
            )

            packets.forEach { (participant, packet) ->
                identityExchangeStarter.ensureStarted(participant.contactId).getOrThrow()
                protocolOutbox.enqueue(participant.contactId, packet).getOrThrow()
            }
        }

    override suspend fun sendMessage(
        conversationId: String,
        text: String
    ) {
        val normalizedText = text.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        val conversation =
            chatDao.findConversationById(conversationId)
                ?: error("Conversation was not found")
        check(conversation.type == DIRECT_CONVERSATION_TYPE) { "Conversation is not direct" }
        val contactId = requireNotNull(conversation.contactId) { "Direct conversation has no contact" }
        val contact = getContact(contactId).getOrThrow() ?: error("Contact was not found")

        identityExchangeStarter.ensureStarted(contactId).getOrThrow()

        val now = SystemClock.nowEpochMilliseconds()
        val messageId = createId(prefix = "message")

        val packet =
            ChatMessagePacket(
                packetId = createId(prefix = "packet"),
                messageId = messageId,
                sentAtEpochMilliseconds = now,
                text = normalizedText
            )

        protocolOutbox
            .enqueue(
                contactId = contactId,
                packet = packet
            ).getOrThrow()

        val plannedTransportMode = contact.plannedTransportMode()

        chatDao.upsertMessage(
            MessageEntity(
                id = messageId,
                conversationId = conversationId,
                packetId = packet.packetId,
                text = normalizedText,
                transportPayload = null,
                transportMode = plannedTransportMode.name,
                contentStatus = MessageContentStatus.READABLE.name,
                deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                senderContactId = null,
                isMine = true,
                createdAtEpochMilliseconds = now
            )
        )

        chatDao.updateConversationTimestamp(
            conversationId = conversationId,
            timestamp = now
        )
    }

    override suspend fun retryMessage(
        messageId: String
    ): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }

            val message = chatDao.findMessageById(messageId) ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            check(message.deliveryStatus == MessageDeliveryStatus.FAILED.name) { "Only failed messages can be retried" }

            val recipientStates = messageRecipientStateDao.findByMessageId(messageId)
            if (recipientStates.isNotEmpty()) {
                recipientStates
                    .filter { it.deliveryStatus == MessageDeliveryStatus.FAILED.name }
                    .forEach { state ->
                        val packetId = state.packetId ?: error("Recipient state has no packet")
                        val outboxItem =
                            protocolOutbox.findByPacketId(packetId).getOrThrow()
                                ?: error("Linked outbox item was not found")
                        protocolOutbox.retry(outboxItem.id).getOrThrow()
                        messageRecipientStateDao.updateDeliveryStatus(
                            messageId = messageId,
                            contactId = state.contactId,
                            deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                            lastError = null,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        )
                    }
            } else {
                val packetId =
                    message.packetId?.takeIf(String::isNotBlank)
                        ?: error("Message has no linked protocol packet")
                val outboxItem =
                    protocolOutbox.findByPacketId(packetId).getOrThrow()
                        ?: error("Linked outbox item was not found")
                protocolOutbox.retry(outboxItem.id).getOrThrow()
            }

            val updatedRows =
                messageDeliveryStatusDao.updateDeliveryStatusByMessageId(
                    messageId = messageId,
                    deliveryStatus = MessageDeliveryStatus.QUEUED.name
                )
            check(updatedRows == 1) { "Message delivery status could not be updated" }
        }

    override suspend fun markConversationRead(
        conversationId: String
    ): Result<Unit> =
        runCatching {
            require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }

            val messages = chatDao.findMessagesAwaitingReadReceipt(conversationId)

            messages.forEach { message ->
                val receipt =
                    ReadReceiptPacket(
                        packetId =
                            createReadReceiptPacketId(
                                messageId = message.messageId
                            ),
                        messageId = message.messageId,
                        readAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )

                protocolOutbox
                    .enqueue(
                        contactId = message.contactId,
                        packet = receipt
                    ).getOrThrow()

                val updatedRows = chatDao.markReadReceiptSent(messageId = message.messageId)

                check(updatedRows == 1) {
                    "Incoming message could not be marked as read"
                }

                println(
                    "Read receipt queued: " +
                        "messageId=${message.messageId}, " +
                        "contactId=${message.contactId}"
                )
            }
        }

    private fun createReadReceiptPacketId(
        messageId: String
    ): String = "read-receipt-$messageId"

    override suspend fun receiveMessage(
        contactId: String,
        encodedTransportPayload: String,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    ) {
        require(encodedTransportPayload.isNotBlank()) {
            "Incoming transport payload must not be blank"
        }

        val receivedAt = SystemClock.nowEpochMilliseconds()
        val decodedTransport =
            incomingTransportMessageDecoder.decode(
                encodedPayload = encodedTransportPayload,
                localPublicKey = localEncryptionPublicKey,
                localPrivateKey = localEncryptionPrivateKey
            )

        when (decodedTransport) {
            is DecodedTransportMessage.Readable -> {
                handleReadableTransportPacket(
                    contactId = contactId,
                    encodedTransportPayload = encodedTransportPayload,
                    decodedTransport = decodedTransport,
                    receivedAt = receivedAt
                )
            }

            is DecodedTransportMessage.InvalidPacket -> {
                storeFailedIncomingMessage(
                    conversation = getOrCreateConversation(contactId),
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Invalid transport packet",
                    transportMode = UNKNOWN_TRANSPORT_MODE,
                    contentStatus = MessageContentStatus.INVALID_PACKET,
                    receivedAt = receivedAt
                )
            }

            is DecodedTransportMessage.InvalidPlaintext -> {
                storeFailedIncomingMessage(
                    conversation = getOrCreateConversation(contactId),
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Unable to read plaintext message",
                    transportMode = TransportEncryptionMode.PLAINTEXT.name,
                    contentStatus = MessageContentStatus.INVALID_PLAINTEXT_PACKET,
                    receivedAt = receivedAt
                )
            }

            is DecodedTransportMessage.DecryptionFailed -> {
                storeFailedIncomingMessage(
                    conversation = getOrCreateConversation(contactId),
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Unable to decrypt secure message",
                    transportMode = TransportEncryptionMode.SEALED_BOX.name,
                    contentStatus = MessageContentStatus.TRANSPORT_DECRYPTION_FAILED,
                    receivedAt = receivedAt
                )
            }
        }
    }

    private suspend fun handleReadableTransportPacket(
        contactId: String,
        encodedTransportPayload: String,
        decodedTransport: DecodedTransportMessage.Readable,
        receivedAt: Long
    ) {
        val packet =
            packetCodec
                .decode(
                    encodedPacket = decodedTransport.plaintext
                ).getOrElse {
                    storeFailedIncomingMessage(
                        conversation = getOrCreateConversation(contactId),
                        encodedTransportPayload = encodedTransportPayload,
                        text = "Invalid protocol packet",
                        transportMode = decodedTransport.mode.name,
                        contentStatus = MessageContentStatus.INVALID_PACKET,
                        receivedAt = receivedAt
                    )

                    return
                }
        val conversationId =
            when (packet) {
                is ChatMessagePacket -> getOrCreateConversation(contactId).id
                is GroupCreatedPacket -> packet.groupId
                is GroupChatMessagePacket -> packet.groupId
                else -> chatDao.findConversationByContactId(contactId)?.id ?: "control-${packet.packetId}"
            }

        protocolPacketHandler
            .handle(
                context =
                    IncomingPacketContext(
                        contactId = contactId,
                        conversationId = conversationId,
                        encodedTransportPayload = encodedTransportPayload,
                        transportMode = decodedTransport.mode.name,
                        receivedAtEpochMilliseconds = receivedAt
                    ),
                packet = packet
            ).onFailure {
                if (packet is ChatMessagePacket) {
                    storeFailedIncomingMessage(
                        conversation = getOrCreateConversation(contactId),
                        encodedTransportPayload = encodedTransportPayload,
                        text = "Unsupported or invalid protocol packet",
                        transportMode = decodedTransport.mode.name,
                        contentStatus = MessageContentStatus.INVALID_PACKET,
                        receivedAt = receivedAt
                    )
                }
            }
    }

    private suspend fun storeFailedIncomingMessage(
        conversation: ConversationEntity,
        encodedTransportPayload: String,
        text: String,
        transportMode: String,
        contentStatus: MessageContentStatus,
        receivedAt: Long
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
                createdAtEpochMilliseconds = receivedAt
            )
        )

        chatDao.updateConversationTimestamp(
            conversationId = conversation.id,
            timestamp = receivedAt
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

        val now = SystemClock.nowEpochMilliseconds()

        val conversation =
            ConversationEntity(
                id = createId(prefix = "conversation"),
                contactId = contactId,
                type = DIRECT_CONVERSATION_TYPE,
                title = null,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )

        chatDao.upsertConversation(conversation)

        return chatDao.findConversationByContactId(
            contactId = contactId
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

    private fun ConversationWithMessages.toDomain(
        participantContactIds: List<String> = emptyList(),
        recipientStates: List<MessageRecipientStateEntity> = emptyList()
    ): Conversation {
        val isGroup = conversation.type == GROUP_CONVERSATION_TYPE
        val contactId = conversation.contactId.orEmpty()
        val statesByMessageId = recipientStates.groupBy { it.messageId }

        return Conversation(
            id = conversation.id,
            contactId = contactId,
            contactName = if (isGroup) conversation.title.orEmpty() else "",
            messages =
                messages
                    .sortedBy(MessageEntity::createdAtEpochMilliseconds)
                    .map { entity ->
                        entity.toDomain(
                            contactId = contactId,
                            recipientStates = statesByMessageId[entity.id].orEmpty()
                        )
                    },
            unreadCount =
                messages.count { message ->
                    !message.isMine &&
                        !message.readReceiptSent &&
                        message.contentStatus == MessageContentStatus.READABLE.name
                },
            isGroup = isGroup,
            participantContactIds = participantContactIds
        )
    }

    private fun MessageEntity.toDomain(
        contactId: String,
        recipientStates: List<MessageRecipientStateEntity> = emptyList()
    ): ChatMessage {
        val deliveryProgress = recipientStates.toDeliveryProgress()
        val aggregatedDeliveryStatus =
            if (recipientStates.isEmpty()) {
                deliveryStatus.toMessageDeliveryStatus()
            } else {
                recipientStates.toAggregatedDeliveryStatus()
            }

        return ChatMessage(
            id = id,
            contactId = contactId,
            text = text,
            isMine = isMine,
            timestamp = createdAtEpochMilliseconds,
            security = transportMode.toMessageSecurity(),
            contentStatus = contentStatus.toMessageContentStatus(),
            deliveryStatus =
                if (isMine) aggregatedDeliveryStatus else MessageDeliveryStatus.NOT_APPLICABLE,
            senderContactId = senderContactId,
            deliveryProgress = deliveryProgress
        )
    }

    private fun ConversationSummary.toDomain(): Conversation {
        val isGroup = conversationType == GROUP_CONVERSATION_TYPE
        val resolvedContactId = contactId.orEmpty()
        val resolvedName =
            if (isGroup) {
                conversationTitle.orEmpty()
            } else {
                contactName?.takeIf(String::isNotBlank) ?: "Unknown contact"
            }
        val lastMessage =
            lastMessageText?.let { text ->
                ChatMessage(
                    id = "summary-$conversationId",
                    contactId = resolvedContactId,
                    text = text,
                    isMine = true,
                    timestamp = lastMessageTimestamp ?: updatedAtEpochMilliseconds,
                    security = MessageSecurity.INSECURE,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE
                )
            }

        return Conversation(
            id = conversationId,
            contactId = resolvedContactId,
            contactName = resolvedName,
            messages = listOfNotNull(lastMessage),
            unreadCount = unreadCount,
            isGroup = isGroup,
            participantContactIds = if (isGroup) List(participantCount) { "" } else emptyList()
        )
    }

    private fun List<MessageRecipientStateEntity>.toDeliveryProgress(): MessageDeliveryProgress =
        MessageDeliveryProgress(
            recipientCount = size,
            deliveredCount =
                count { state ->
                    state.deliveryStatus == MessageDeliveryStatus.DELIVERED.name ||
                        state.deliveryStatus == MessageDeliveryStatus.READ.name
                },
            readCount = count { state -> state.deliveryStatus == MessageDeliveryStatus.READ.name }
        )

    private fun List<MessageRecipientStateEntity>.toAggregatedDeliveryStatus(): MessageDeliveryStatus {
        if (isEmpty()) return MessageDeliveryStatus.NOT_APPLICABLE
        if (all { it.deliveryStatus == MessageDeliveryStatus.READ.name }) return MessageDeliveryStatus.READ
        if (all { it.deliveryStatus == MessageDeliveryStatus.DELIVERED.name || it.deliveryStatus == MessageDeliveryStatus.READ.name }) {
            return MessageDeliveryStatus.DELIVERED
        }
        if (all { it.deliveryStatus == MessageDeliveryStatus.FAILED.name }) return MessageDeliveryStatus.FAILED
        if (any { it.deliveryStatus == MessageDeliveryStatus.SENDING.name }) return MessageDeliveryStatus.SENDING
        if (all { it.deliveryStatus == MessageDeliveryStatus.SENT.name || it.deliveryStatus == MessageDeliveryStatus.DELIVERED.name || it.deliveryStatus == MessageDeliveryStatus.READ.name }) {
            return MessageDeliveryStatus.SENT
        }
        return MessageDeliveryStatus.QUEUED
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
                newValue = ""
            )

        return "$prefix-$timestamp-$random"
    }

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val GROUP_OWNER_ROLE = "OWNER"
        const val GROUP_MEMBER_ROLE = "MEMBER"
        const val MIN_GROUP_PARTICIPANT_COUNT = 1
        const val UNKNOWN_TRANSPORT_MODE = "UNKNOWN"
    }
}
