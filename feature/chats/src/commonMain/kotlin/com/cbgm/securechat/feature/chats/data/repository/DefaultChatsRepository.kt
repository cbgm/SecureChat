package com.cbgm.securechat.feature.chats.data.repository

import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.id.IdGenerator
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
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.data.conversation.DirectConversationStore
import com.cbgm.securechat.feature.chats.data.delivery.MessageDeliveryStateCoordinator
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.GroupConversation
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryProgress
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStateMachine
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DefaultChatsRepository(
    private val chatDao: ChatDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val directConversationStore: DirectConversationStore,
    private val deliveryStateCoordinator: MessageDeliveryStateCoordinator,
    private val getContact: GetContact,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox
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

    override suspend fun getOrCreateDirectConversation(contactId: String): String = directConversationStore.getOrCreate(contactId).id

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
        val conversationId = IdGenerator.generate(prefix = "group")
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
                            displayName = null,
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
            protocolOutbox
                .enqueue(
                    contactId = contact.id,
                    packet =
                        GroupCreatedPacket(
                            packetId = IdGenerator.generate(prefix = "group-created-packet"),
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
            val messageId = IdGenerator.generate(prefix = "group-message")
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
            val packets =
                participants.associateWith { participant ->
                    GroupChatMessagePacket(
                        packetId = IdGenerator.generate(prefix = "group-message-packet"),
                        groupId = conversationId,
                        messageId = messageId,
                        sentAtEpochMilliseconds = now,
                        text = normalizedText,
                        senderSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                        senderPhoneNumber = localPhoneNumber
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
                val enqueueResult = protocolOutbox.enqueue(participant.contactId, packet)

                if (enqueueResult.isFailure) {
                    val error = enqueueResult.exceptionOrNull()

                    deliveryStateCoordinator.applyPacketEvent(
                        packetId = packet.packetId,
                        event = MessageDeliveryEvent.SEND_FAILED,
                        errorMessage = error?.message
                    )

                    throw error ?: IllegalStateException("Group message could not be queued")
                }
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

        val now = SystemClock.nowEpochMilliseconds()
        val messageId = IdGenerator.generate(prefix = "message")
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()

        val packet =
            ChatMessagePacket(
                packetId = IdGenerator.generate(prefix = "packet"),
                messageId = messageId,
                sentAtEpochMilliseconds = now,
                text = normalizedText,
                senderPhoneNumber = localPhoneNumber
            )

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

        val enqueueResult =
            protocolOutbox.enqueue(
                contactId = contactId,
                packet = packet
            )

        if (enqueueResult.isFailure) {
            val error = enqueueResult.exceptionOrNull()

            deliveryStateCoordinator.applyPacketEvent(
                packetId = packet.packetId,
                event = MessageDeliveryEvent.SEND_FAILED,
                errorMessage = error?.message
            )

            throw error ?: IllegalStateException("Message could not be queued")
        }
    }

    override suspend fun retryMessage(
        messageId: String
    ): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }

            val message = chatDao.findMessageById(messageId) ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            check(
                MessageDeliveryStateMachine.canTransition(
                    current = message.deliveryStatus.toMessageDeliveryStatus(),
                    event = MessageDeliveryEvent.RETRY_REQUESTED
                )
            ) {
                "Only failed messages can be retried"
            }

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
                        deliveryStateCoordinator.applyRetryEvent(
                            messageId = messageId,
                            contactId = state.contactId
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
                deliveryStateCoordinator.applyRetryEvent(messageId = messageId)
            }
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
        val statuses = map { state -> state.deliveryStatus.toMessageDeliveryStatus() }
        return MessageDeliveryStateMachine.aggregate(statuses)
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

    private companion object {
        const val DIRECT_CONVERSATION_TYPE = "DIRECT"
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val GROUP_OWNER_ROLE = "OWNER"
        const val GROUP_MEMBER_ROLE = "MEMBER"
        const val MIN_GROUP_PARTICIPANT_COUNT = 1
    }
}
