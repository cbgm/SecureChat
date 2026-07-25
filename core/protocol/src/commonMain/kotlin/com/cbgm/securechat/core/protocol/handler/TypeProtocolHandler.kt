package com.cbgm.securechat.core.protocol.handler

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

/**
 * Handles one or more concrete SecureChat packet types.
 *
 * Implementations belong to feature modules:
 *
 * feature:chats
 * -> ChatMessagePacketHandler
 *
 * feature:contacts
 * -> IdentityPacketHandler
 * -> IdentityAcknowledgementPacketHandler
 */
interface TypedProtocolPacketHandler {
    fun canHandle(packet: SecureChatPacket): Boolean

    suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit>
}
