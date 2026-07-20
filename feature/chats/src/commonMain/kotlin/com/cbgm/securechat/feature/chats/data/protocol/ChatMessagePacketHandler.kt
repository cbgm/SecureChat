package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class ChatMessagePacketHandler(
    private val chatDao: ChatDao,
    private val protocolOutbox: ProtocolOutbox
) : TypedProtocolPacketHandler {

    override fun canHandle(
        packet: SecureChatPacket
    ): Boolean {
        return packet is ChatMessagePacket
    }

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> {
        return runCatching {
            val chatPacket = packet as? ChatMessagePacket
                ?: error("ChatMessagePacketHandler received an incompatible packet")

            require(chatPacket.text.isNotBlank()) {
                "Incoming chat message must not be blank"
            }

            /*
             * messageId is the Room primary key.
             *
             * Repeated delivery of the same message updates the same
             * database row rather than creating duplicates.
             */
            val conversation = chatDao.findConversationByContactId(contactId = context.contactId)
                ?: ConversationEntity(
                    id = context.conversationId,
                    contactId = context.contactId,
                    createdAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                )

            val incomingMessage = MessageEntity(
                id = chatPacket.messageId,
                conversationId = conversation.id,
                packetId = chatPacket.packetId,
                text = chatPacket.text,
                transportPayload = context.encodedTransportPayload,
                transportMode = context.transportMode,
                contentStatus = MessageContentStatus.READABLE.name,
                deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
                isMine = false,
                createdAtEpochMilliseconds = chatPacket.sentAtEpochMilliseconds
            )

            chatDao.upsertIncomingChatMessage(
                conversation = conversation,
                message = incomingMessage,
                timestamp = context.receivedAtEpochMilliseconds
            )

            /*
             * A receipt is generated only after the message has been
             * successfully persisted.
             *
             * The deterministic packet ID makes the operation
             * idempotent. Repeated delivery of the same chat message
             * will not create multiple receipt outbox rows.
             */
            val receipt = DeliveryReceiptPacket(
                packetId = createDeliveryReceiptPacketId(
                    messageId =
                        chatPacket.messageId
                ),

                messageId = chatPacket.messageId,
                deliveredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )

            protocolOutbox.enqueue(
                contactId = context.contactId,
                packet = receipt
            ).getOrThrow()

            println(
                "Delivery receipt queued: " +
                        "messageId=${chatPacket.messageId}, " +
                        "contactId=${context.contactId}"
            )
        }
    }

    private fun createDeliveryReceiptPacketId(
        messageId: String
    ): String {
        return "delivery-receipt-$messageId"
    }
}