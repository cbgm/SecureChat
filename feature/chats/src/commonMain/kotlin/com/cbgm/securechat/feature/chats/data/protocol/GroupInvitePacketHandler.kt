package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationCoordinator

class GroupInvitePacketHandler(
    private val groupInvitationCoordinator: GroupInvitationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        groupInvitationCoordinator.receiveInvite(
            ownerContactId = context.contactId,
            packet = packet as GroupInvitePacket,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        )
}
