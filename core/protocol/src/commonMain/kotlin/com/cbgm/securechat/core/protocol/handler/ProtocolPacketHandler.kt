package com.cbgm.securechat.core.protocol.handler

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

interface ProtocolPacketHandler {

    suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit>
}