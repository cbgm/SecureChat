package com.cbgm.securechat.feature.contacts.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.data.identity.ContactVerificationCoordinator

class ContactVerificationReceiptPacketHandler(
    private val coordinator: ContactVerificationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is ContactVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveReceipt(
            context = context,
            packet = packet as? ContactVerificationReceiptPacket ?: error("Incompatible contact verification receipt")
        )
}
