package com.cbgm.securechat.feature.chats.data.incoming

import com.cbgm.securechat.core.crypto.transport.DecodedTransportMessage
import com.cbgm.securechat.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.ProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.data.conversation.DirectConversationStore
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class IncomingMessageProcessor(
    private val chatDao: ChatDao,
    private val directConversationStore: DirectConversationStore,
    private val transportMessageDecoder: IncomingTransportMessageDecoder,
    private val packetCodec: PacketCodec,
    private val packetHandler: ProtocolPacketHandler
) : IncomingMessageHandler {
    override suspend fun handle(
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
            transportMessageDecoder.decode(
                encodedPayload = encodedTransportPayload,
                localPublicKey = localEncryptionPublicKey,
                localPrivateKey = localEncryptionPrivateKey
            )

        when (decodedTransport) {
            is DecodedTransportMessage.Readable ->
                processReadablePacket(
                    contactId = contactId,
                    encodedTransportPayload = encodedTransportPayload,
                    decodedTransport = decodedTransport,
                    receivedAt = receivedAt
                )

            is DecodedTransportMessage.InvalidPacket ->
                storeUnreadableMessage(
                    contactId = contactId,
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Invalid transport packet",
                    transportMode = UNKNOWN_TRANSPORT_MODE,
                    contentStatus = MessageContentStatus.INVALID_PACKET,
                    receivedAt = receivedAt
                )

            is DecodedTransportMessage.InvalidPlaintext ->
                storeUnreadableMessage(
                    contactId = contactId,
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Unable to read plaintext message",
                    transportMode = TransportEncryptionMode.PLAINTEXT.name,
                    contentStatus = MessageContentStatus.INVALID_PLAINTEXT_PACKET,
                    receivedAt = receivedAt
                )

            is DecodedTransportMessage.DecryptionFailed ->
                storeUnreadableMessage(
                    contactId = contactId,
                    encodedTransportPayload = encodedTransportPayload,
                    text = "Unable to decrypt secure message",
                    transportMode = TransportEncryptionMode.SEALED_BOX.name,
                    contentStatus = MessageContentStatus.TRANSPORT_DECRYPTION_FAILED,
                    receivedAt = receivedAt
                )
        }
    }

    private suspend fun processReadablePacket(
        contactId: String,
        encodedTransportPayload: String,
        decodedTransport: DecodedTransportMessage.Readable,
        receivedAt: Long
    ) {
        val packet =
            packetCodec
                .decode(decodedTransport.plaintext)
                .getOrElse { error ->
                    throw IllegalArgumentException("Invalid protocol packet", error)
                }
        val context =
            IncomingPacketContext(
                contactId = contactId,
                conversationId = resolveConversationId(packet, contactId),
                encodedTransportPayload = encodedTransportPayload,
                transportMode = decodedTransport.mode.name,
                receivedAtEpochMilliseconds = receivedAt
            )

        val handlingResult =
            packetHandler.handle(
                context = context,
                packet = packet
            )

        if (handlingResult.isFailure) {
            val error = handlingResult.exceptionOrNull()

            if (packet !is ChatMessagePacket) {
                throw error ?: IllegalStateException("Protocol packet could not be handled")
            }

            storeUnreadableMessage(
                contactId = contactId,
                encodedTransportPayload = encodedTransportPayload,
                text = "Unsupported or invalid protocol packet",
                transportMode = decodedTransport.mode.name,
                contentStatus = MessageContentStatus.INVALID_PACKET,
                receivedAt = receivedAt
            )
        }
    }

    private suspend fun resolveConversationId(
        packet: SecureChatPacket,
        contactId: String
    ): String =
        when (packet) {
            is ChatMessagePacket -> directConversationStore.getOrCreate(contactId).id
            is GroupCreatedPacket -> packet.groupId
            is GroupMemberActivatedPacket -> packet.groupId
            is GroupMemberActivationAcknowledgementPacket -> packet.groupId
            is GroupChatMessagePacket -> packet.groupId
            is GroupInvitePacket -> packet.groupId
            is GroupJoinRequestPacket -> packet.groupId
            is GroupInviteDeclinedPacket -> packet.groupId
            is GroupReadyAcknowledgementPacket -> packet.groupId
            else -> chatDao.findConversationByContactId(contactId)?.id ?: "control-${packet.packetId}"
        }

    private suspend fun storeUnreadableMessage(
        contactId: String,
        encodedTransportPayload: String,
        text: String,
        transportMode: String,
        contentStatus: MessageContentStatus,
        receivedAt: Long
    ) {
        val conversation = directConversationStore.getOrCreate(contactId)

        chatDao.upsertMessage(
            MessageEntity(
                id = IdGenerator.generate(prefix = "failed-message"),
                conversationId = conversation.id,
                packetId = null,
                text = text,
                transportPayload = encodedTransportPayload,
                transportMode = transportMode,
                contentStatus = contentStatus.name,
                deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
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

    private companion object {
        const val UNKNOWN_TRANSPORT_MODE = "UNKNOWN"
    }
}
