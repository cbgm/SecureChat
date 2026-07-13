package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class ChatMessagePacketHandler(
    private val chatDao: ChatDao
) : TypedProtocolPacketHandler {

    override fun canHandle(
        packet: SecureChatPacket
    ): Boolean {
        return packet is ChatMessagePacket
    }

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> {
        return runCatching {
            val chatPacket =
                packet as?
                        ChatMessagePacket
                    ?: error(
                        "ChatMessagePacketHandler received an incompatible packet"
                    )

            chatDao.upsertMessage(
                MessageEntity(
                    id =
                        chatPacket.messageId,

                    conversationId =
                        context.conversationId,

                    packetId =
                        chatPacket.packetId,

                    text =
                        chatPacket.text,

                    transportPayload =
                        context
                            .encodedTransportPayload,

                    transportMode =
                        context.transportMode,

                    contentStatus =
                        MessageContentStatus
                            .READABLE
                            .name,

                    deliveryStatus =
                        MessageDeliveryStatus
                            .NOT_APPLICABLE
                            .name,

                    isMine =
                        false,

                    createdAtEpochMilliseconds =
                        chatPacket
                            .sentAtEpochMilliseconds
                )
            )

            chatDao.updateConversationTimestamp(
                conversationId =
                    context.conversationId,

                timestamp =
                    context
                        .receivedAtEpochMilliseconds
            )
        }
    }
}