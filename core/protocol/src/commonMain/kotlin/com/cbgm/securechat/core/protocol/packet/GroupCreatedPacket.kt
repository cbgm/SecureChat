package com.cbgm.securechat.core.protocol.packet

import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_created")
data class GroupCreatedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val title: String,
    val createdAtEpochMilliseconds: Long,
    val members: List<GroupMemberPayload>
) : SecureChatPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(title.isNotBlank()) { "Group title must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Group timestamp must not be negative" }
        require(members.size >= 2) { "A group packet requires at least two identities" }
    }
}
