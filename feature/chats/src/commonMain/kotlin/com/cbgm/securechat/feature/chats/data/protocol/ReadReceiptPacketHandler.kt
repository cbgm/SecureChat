package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao

class ReadReceiptPacketHandler(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao
) : TypedProtocolPacketHandler {

    override fun canHandle(
        packet: SecureChatPacket
    ): Boolean {
        return packet is ReadReceiptPacket
    }

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> {
        return runCatching {
            val receipt =
                packet as? ReadReceiptPacket
                    ?: error(
                        "ReadReceiptPacketHandler received an incompatible packet"
                    )

            val updatedRows =
                messageDeliveryStatusDao
                    .markOutgoingMessageRead(
                        messageId =
                            receipt.messageId,

                        contactId =
                            context.contactId
                    )

            println(
                "Read receipt handled: " +
                        "messageId=${receipt.messageId}, " +
                        "contactId=${context.contactId}, " +
                        "updatedRows=$updatedRows"
            )
        }
    }
}