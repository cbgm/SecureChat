package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationCoordinator

class GroupInviteDeclinedPacketHandler(
    private val groupInvitationCoordinator: GroupInvitationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        groupInvitationCoordinator.receiveDecline(
            memberContactId = context.contactId,
            packet = packet as GroupInviteDeclinedPacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
