package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class GroupChatMessagePacketHandler(
    private val chatDao: ChatDao,
    private val protocolOutbox: ProtocolOutbox
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupChatMessagePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val groupPacket =
                packet as? GroupChatMessagePacket
                    ?: error("GroupChatMessagePacketHandler received an incompatible packet")
            val conversation =
                chatDao.findConversationById(groupPacket.groupId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }

            chatDao.upsertMessage(
                MessageEntity(
                    id = groupPacket.messageId,
                    conversationId = groupPacket.groupId,
                    packetId = groupPacket.packetId,
                    text = groupPacket.text,
                    transportPayload = context.encodedTransportPayload,
                    transportMode = context.transportMode,
                    contentStatus = MessageContentStatus.READABLE.name,
                    deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
                    senderContactId = context.contactId,
                    isMine = false,
                    createdAtEpochMilliseconds = groupPacket.sentAtEpochMilliseconds
                )
            )
            chatDao.updateConversationTimestamp(groupPacket.groupId, context.receivedAtEpochMilliseconds)

            protocolOutbox
                .enqueue(
                    contactId = context.contactId,
                    packet =
                        DeliveryReceiptPacket(
                            packetId = "delivery-receipt-${groupPacket.messageId}-${context.contactId}",
                            messageId = groupPacket.messageId,
                            deliveredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        )
                ).getOrThrow()
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
