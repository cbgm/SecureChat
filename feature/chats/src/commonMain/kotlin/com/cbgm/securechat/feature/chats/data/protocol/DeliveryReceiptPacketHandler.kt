package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class DeliveryReceiptPacketHandler(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is DeliveryReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket,
    ): Result<Unit> =
        runCatching {
            val receipt =
                packet as? DeliveryReceiptPacket
                    ?: error("DeliveryReceiptPacketHandler received an incompatible packet")

            /*
             * Updating zero rows is not automatically an error.
             *
             * It can happen when:
             * - the receipt is duplicated;
             * - the message was deleted locally;
             * - the message is already DELIVERED or READ.
             */
            val updatedRows =
                messageDeliveryStatusDao.markOutgoingMessageDelivered(
                    messageId = receipt.messageId,
                    contactId = context.contactId,
                )

            println(
                "Delivery receipt handled: " +
                    "messageId=${receipt.messageId}, " +
                    "contactId=${context.contactId}, " +
                    "updatedRows=$updatedRows",
            )
        }
}
