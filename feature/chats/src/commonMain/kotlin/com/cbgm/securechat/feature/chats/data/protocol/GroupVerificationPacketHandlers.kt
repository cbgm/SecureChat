package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.verification.GroupVerificationCoordinator

class GroupVerificationReceiptPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveReceipt(
            context = context,
            packet =
                packet as? GroupVerificationReceiptPacket
                    ?: error("Incompatible group verification receipt")
        )
}

class GroupVerificationSnapshotRequestPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupVerificationSnapshotRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveSnapshotRequest(
            context = context,
            packet =
                packet as? GroupVerificationSnapshotRequestPacket
                    ?: error("Incompatible group verification snapshot request")
        )
}

class GroupVerificationSnapshotPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupVerificationSnapshotPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveSnapshot(
            context = context,
            packet =
                packet as? GroupVerificationSnapshotPacket
                    ?: error("Incompatible group verification snapshot")
        )
}
