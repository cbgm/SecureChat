package com.cbgm.securechat.core.protocol.handler

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

class DefaultProtocolPacketHandler(
    private val handlers: List<TypedProtocolPacketHandler>
) : ProtocolPacketHandler {
    private val logger = SecureChatLog.withTag("DefaultProtocolPacketHandler")

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val matchingHandler =
                handlers.firstOrNull { handler ->
                    handler.canHandle(packet = packet)
                } ?: error("No handler registered for " + packet::class.simpleName)

            logger.debug {
                "Handling protocol packet: " +
                    "type=${packet::class.simpleName}, " +
                    "packetId=${packet.packetId}, " +
                    "contactId=${context.contactId}"
            }

            matchingHandler
                .handle(
                    context = context,
                    packet = packet
                ).getOrThrow()

            logger.debug {
                "Protocol packet handled successfully: " +
                    "type=${packet::class.simpleName}, " +
                    "packetId=${packet.packetId}"
            }
        }.onFailure { error ->
            logger.error(error) {
                "Protocol packet handling failed: " +
                    "type=${packet::class.simpleName}, " +
                    "packetId=${packet.packetId}"
            }
        }
}
