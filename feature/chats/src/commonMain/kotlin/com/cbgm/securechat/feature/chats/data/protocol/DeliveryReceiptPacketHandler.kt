package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class DeliveryReceiptPacketHandler(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
    private val messageRecipientStateDao: MessageRecipientStateDao
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is DeliveryReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val receipt = packet as? DeliveryReceiptPacket ?: error("DeliveryReceiptPacketHandler received an incompatible packet")
            val state =
                messageRecipientStateDao
                    .findByMessageId(receipt.messageId)
                    .firstOrNull { it.contactId == context.contactId }

            if (state == null) {
                messageDeliveryStatusDao.markOutgoingMessageDelivered(receipt.messageId, context.contactId)
                return@runCatching
            }

            messageRecipientStateDao.updateDeliveryStatus(
                messageId = receipt.messageId,
                contactId = context.contactId,
                deliveryStatus = MessageDeliveryStatus.DELIVERED.name,
                lastError = null,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            val states = messageRecipientStateDao.findByMessageId(receipt.messageId)
            val aggregated =
                when {
                    states.all { it.deliveryStatus == MessageDeliveryStatus.READ.name } -> MessageDeliveryStatus.READ
                    states.all { it.deliveryStatus == MessageDeliveryStatus.DELIVERED.name || it.deliveryStatus == MessageDeliveryStatus.READ.name } -> MessageDeliveryStatus.DELIVERED
                    else -> MessageDeliveryStatus.SENT
                }
            messageDeliveryStatusDao.updateDeliveryStatusByMessageId(receipt.messageId, aggregated.name)
        }
}
