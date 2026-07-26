package com.cbgm.securechat.core.protocol.packet

import com.cbgm.securechat.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_chat_message")
data class GroupChatMessagePacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val messageId: String,
    val sentAtEpochMilliseconds: Long,
    val text: String,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val senderSigningPublicKey: ByteArray = byteArrayOf(),
    val senderPhoneNumber: String? = null
) : SecureChatPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(sentAtEpochMilliseconds >= 0L) { "Message timestamp must not be negative" }
        require(text.isNotBlank()) { "Message text must not be blank" }
    }
}
