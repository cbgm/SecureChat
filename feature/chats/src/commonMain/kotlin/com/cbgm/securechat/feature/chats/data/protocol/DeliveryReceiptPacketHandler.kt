package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.delivery.MessageDeliveryStateCoordinator
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

class DeliveryReceiptPacketHandler(
    private val deliveryStateCoordinator: MessageDeliveryStateCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is DeliveryReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val receipt = packet as? DeliveryReceiptPacket ?: error("DeliveryReceiptPacketHandler received an incompatible packet")
            deliveryStateCoordinator.applyReceiptEvent(
                messageId = receipt.messageId,
                contactId = context.contactId,
                event = MessageDeliveryEvent.DELIVERY_CONFIRMED
            )
        }
}
