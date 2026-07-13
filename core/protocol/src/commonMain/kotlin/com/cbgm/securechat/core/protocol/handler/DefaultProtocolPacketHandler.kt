package com.cbgm.securechat.core.protocol.handler

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

class DefaultProtocolPacketHandler(
    private val handlers:
    List<TypedProtocolPacketHandler>
) : ProtocolPacketHandler {

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> {
        return runCatching {
            val matchingHandler =
                handlers.firstOrNull { handler ->
                    handler.canHandle(
                        packet = packet
                    )
                }
                    ?: error(
                        "Unsupported protocol packet type: " +
                                packet::class.simpleName
                    )

            matchingHandler
                .handle(
                    context = context,
                    packet = packet
                )
                .getOrThrow()
        }
    }
}