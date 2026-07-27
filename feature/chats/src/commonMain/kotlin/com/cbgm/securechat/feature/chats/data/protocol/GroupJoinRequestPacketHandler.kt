package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationCoordinator

class GroupJoinRequestPacketHandler(
    private val groupInvitationCoordinator: GroupInvitationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupJoinRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        groupInvitationCoordinator.receiveJoinRequest(
            memberContactId = context.contactId,
            packet = packet as GroupJoinRequestPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
